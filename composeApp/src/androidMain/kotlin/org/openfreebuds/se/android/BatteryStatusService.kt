package org.openfreebuds.se.android

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.openfreebuds.se.connection.ConnectionState

/**
 * Foreground service that keeps the app process alive while the permanent
 * battery notification is enabled. Owns the process-wide [SeEngine] so the
 * periodic battery polls keep the notification and the home-screen widget
 * fresh in the background, independently of the activity lifecycle.
 *
 * The battery notification is shown only while the earbuds are connected and
 * is hidden whenever the link is lost.
 */
class BatteryStatusService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        SeEngine.ensureStarted(this)
        val controller = SeEngine.controller
        stateJob = serviceScope.launch {
            controller.state.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> showBatteryNotification()
                    else -> hideBatteryNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val controller = SeEngine.controller
        // Always start as foreground promptly on Android 8+; onStartCommand must
        // call startForeground quickly or the system throws.
        showBatteryNotification()
        if (controller.state.value !is ConnectionState.Connected) {
            hideBatteryNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stateJob?.cancel()
        serviceScope.cancel()
        BatteryNotification.hide(this)
        super.onDestroy()
    }

    private fun showBatteryNotification() {
        val controller = SeEngine.controller
        BatteryNotification.markActive(this)
        startForegroundCompat(
            BatteryNotification.NOTIFICATION_ID,
            BatteryNotification.build(this, controller.battery.value),
        )
    }

    private fun hideBatteryNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(ServiceCompat.STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        BatteryNotification.hide(this)
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(id, notification)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, BatteryStatusService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryStatusService::class.java))
        }
    }
}