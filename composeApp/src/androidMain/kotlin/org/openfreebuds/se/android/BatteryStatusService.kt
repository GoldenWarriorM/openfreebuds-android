package org.openfreebuds.se.android

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import org.openfreebuds.se.model.BatteryLevels

/**
 * Foreground service that keeps the app process alive while the permanent
 * battery notification is enabled, so periodic battery polls keep the
 * notification and the home-screen widget fresh in the background.
 */
class BatteryStatusService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        BatteryNotification.markActive(this)
        val notification = BatteryNotification.build(this, BatteryNotification.lastLevels)
        startForegroundCompat(BatteryNotification.NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        BatteryNotification.hide(this)
        super.onDestroy()
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
