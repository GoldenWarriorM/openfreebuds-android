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
 * The service stays in the foreground at all times while it is running, so the
 * system does not kill it. The notification content reflects the connection
 * state: battery levels while connected, "Not connected" otherwise. The user
 * can hide the notification via the system settings (channel), like with any
 * other keep-alive notification.
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
                val connected = state is ConnectionState.Connected
                BatteryNotification.connected = connected
                if (connected) {
                    BatteryNotification.show(
                        this@BatteryStatusService,
                        controller.battery.value,
                    )
                } else {
                    BatteryNotification.hideBattery(this@BatteryStatusService)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always start as foreground promptly on Android 8+; onStartCommand must
        // call startForeground quickly or the system throws.
        updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        stateJob?.cancel()
        serviceScope.cancel()
        BatteryNotification.hide(this)
        super.onDestroy()
    }

    private fun updateNotification() {
        BatteryNotification.markActive(this)
        val controller = SeEngine.controller
        val connected = controller.state.value is ConnectionState.Connected
        BatteryNotification.connected = connected
        if (connected) {
            BatteryNotification.show(this, controller.battery.value)
        } else {
            BatteryNotification.hideBattery(this)
        }
        startForegroundCompat(
            BatteryNotification.SERVICE_ID,
            BatteryNotification.serviceNotification(this),
        )
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
