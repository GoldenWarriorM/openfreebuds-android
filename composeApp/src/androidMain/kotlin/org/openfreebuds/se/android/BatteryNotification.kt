package org.openfreebuds.se.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.openfreebuds.se.R
import org.openfreebuds.se.model.BatteryLevels

/**
 * Shows an ongoing (persistent) notification with the current battery levels
 * of both earbuds and the charging case, while the app is running.
 */
object BatteryNotification {

    const val SERVICE_CHANNEL_ID = "service_status"
    const val BATTERY_CHANNEL_ID = "battery_status"
    const val SERVICE_ID = 2
    const val BATTERY_ID = 4
    const val SERVICE_NAME = "FreeBuds SE Service"
    const val SERVICE_TEXT =
        "FreeBuds SE is running in the background\nTo hide this, hold and tap \"Turn off\""

    var lastLevels: BatteryLevels = BatteryLevels.Unknown
        private set
    var connected: Boolean = true
    private var serviceVisible = false
    private var batteryVisible = false

    val currentLevels: BatteryLevels get() = lastLevels

    /** Shows the persistent keep-alive foreground-service notification. */
    fun markActive(context: Context) {
        ensureChannel(context)
        serviceVisible = true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(SERVICE_ID, buildService(context))
    }

    fun hideBattery(context: Context) {
        batteryVisible = false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(BATTERY_ID)
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            SERVICE_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = SERVICE_TEXT
            setShowBadge(false)
        }
        val batteryChannel = NotificationChannel(
            BATTERY_CHANNEL_ID,
            "Battery status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Current battery level of the earbuds and case"
            setShowBadge(false)
        }
        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(batteryChannel)
    }

    fun show(context: Context, battery: BatteryLevels) {
        if (!canPost(context)) return
        ensureChannel(context)
        lastLevels = battery
        batteryVisible = true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(BATTERY_ID, build(context, battery))
    }

    fun update(context: Context, battery: BatteryLevels) {
        if (!batteryVisible || !canPost(context)) return
        lastLevels = battery
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(BATTERY_ID, build(context, battery))
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS",
            ) == PackageManager.PERMISSION_GRANTED

    fun hide(context: Context) {
        serviceVisible = false
        batteryVisible = false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(SERVICE_ID)
        manager.cancel(BATTERY_ID)
    }

    /** The permanent foreground-service keep-alive notification. */
    fun serviceNotification(context: Context): Notification = buildService(context)

    private fun buildService(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle(SERVICE_NAME)
            .setContentText(SERVICE_TEXT)
            .setStyle(Notification.BigTextStyle().bigText(SERVICE_TEXT))
            .setSmallIcon(R.drawable.ic_case)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setShowWhen(false)
            .setWhen(0L)
            .build()
    }

    fun build(context: Context, battery: BatteryLevels): Notification {
        val content = buildList {
            add("Left: ${format(battery.left)}")
            add("Right: ${format(battery.right)}")
            add("Case: ${format(battery.case)}")
        }.joinToString("  •  ")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(context, BATTERY_CHANNEL_ID)
            .setContentTitle("FreeBuds SE")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_case)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(false)
            .build()
    }

    private fun format(level: Int?): String = if (level != null) "$level%" else "—"
}
