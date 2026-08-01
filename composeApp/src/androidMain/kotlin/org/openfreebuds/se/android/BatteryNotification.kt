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

    const val CHANNEL_ID = "battery_status"
    const val NOTIFICATION_ID = 2

    var lastLevels: BatteryLevels = BatteryLevels.Unknown
        private set
    private var visible = false

    val currentLevels: BatteryLevels get() = lastLevels

    /** Marks the notification as active (called when the foreground service starts). */
    fun markActive(context: Context) {
        ensureChannel(context)
        visible = true
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Battery status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Current battery level of the earbuds and case"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, battery: BatteryLevels) {
        if (!canPost(context)) return
        ensureChannel(context)
        lastLevels = battery
        visible = true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, build(context, battery))
    }

    fun update(context: Context, battery: BatteryLevels) {
        if (!visible || !canPost(context)) return
        lastLevels = battery
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, build(context, battery))
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS",
            ) == PackageManager.PERMISSION_GRANTED

    fun hide(context: Context) {
        visible = false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
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

        return Notification.Builder(context, CHANNEL_ID)
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
