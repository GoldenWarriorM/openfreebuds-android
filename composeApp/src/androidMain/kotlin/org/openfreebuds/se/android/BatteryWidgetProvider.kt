package org.openfreebuds.se.android

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import org.openfreebuds.se.R
import org.openfreebuds.se.model.BatteryLevels

/**
 * Home screen widget showing left/right/case battery levels.
 *
 * Data is persisted to [PREFS_NAME] whenever new battery info arrives
 * (see [saveAndUpdate]) and re-rendered in [onUpdate].
 */
class BatteryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val data = WidgetData.load(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, data))
        }
    }

    companion object {
        const val PREFS_NAME = "battery_widget"

        private const val KEY_LEFT = "left"
        private const val KEY_RIGHT = "right"
        private const val KEY_CASE = "case"
        private const val KEY_CHARGING_LEFT = "charging_left"
        private const val KEY_CHARGING_RIGHT = "charging_right"
        private const val KEY_CHARGING_CASE = "charging_case"

        data class WidgetData(
            val left: Int?,
            val right: Int?,
            val case: Int?,
            val chargingLeft: Boolean,
            val chargingRight: Boolean,
            val chargingCase: Boolean,
        ) {
            companion object {
                fun load(context: Context): WidgetData {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    return WidgetData(
                        left = prefs.getInt(KEY_LEFT, -1).takeIf { it >= 0 },
                        right = prefs.getInt(KEY_RIGHT, -1).takeIf { it >= 0 },
                        case = prefs.getInt(KEY_CASE, -1).takeIf { it >= 0 },
                        chargingLeft = prefs.getBoolean(KEY_CHARGING_LEFT, false),
                        chargingRight = prefs.getBoolean(KEY_CHARGING_RIGHT, false),
                        chargingCase = prefs.getBoolean(KEY_CHARGING_CASE, false),
                    )
                }
            }
        }

        /** Persists the latest battery levels and updates every widget instance. */
        fun saveAndUpdate(context: Context, battery: BatteryLevels) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_LEFT, battery.left ?: -1)
                .putInt(KEY_RIGHT, battery.right ?: -1)
                .putInt(KEY_CASE, battery.case ?: -1)
                .putBoolean(KEY_CHARGING_LEFT, battery.chargingLeft)
                .putBoolean(KEY_CHARGING_RIGHT, battery.chargingRight)
                .putBoolean(KEY_CHARGING_CASE, battery.chargingCase)
                .apply()

            val data = WidgetData(
                battery.left,
                battery.right,
                battery.case,
                battery.chargingLeft,
                battery.chargingRight,
                battery.chargingCase,
            )
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, BatteryWidgetProvider::class.java),
            )
            ids.forEach { id ->
                manager.updateAppWidget(id, buildViews(context, data))
            }
        }

        private fun buildViews(context: Context, data: WidgetData): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_battery)

            configureRow(
                views,
                iconId = R.id.row_left_icon,
                progressId = R.id.row_left_progress,
                percentId = R.id.row_left_percent,
                level = data.left,
                charging = data.chargingLeft,
            )
            configureRow(
                views,
                iconId = R.id.row_right_icon,
                progressId = R.id.row_right_progress,
                percentId = R.id.row_right_percent,
                level = data.right,
                charging = data.chargingRight,
            )
            configureRow(
                views,
                iconId = R.id.row_case_icon,
                progressId = R.id.row_case_progress,
                percentId = R.id.row_case_percent,
                level = data.case,
                charging = data.chargingCase,
            )
            return views
        }

        private fun configureRow(
            views: RemoteViews,
            iconId: Int,
            progressId: Int,
            percentId: Int,
            level: Int?,
            charging: Boolean,
        ) {
            if (level == null) {
                views.setTextViewText(percentId, "—")
                views.setProgressBar(progressId, 100, 0, false)
            } else {
                views.setTextViewText(
                    percentId,
                    if (charging) "$level% ⚡" else "$level%",
                )
                views.setProgressBar(progressId, 100, level.coerceIn(0, 100), false)
            }
            views.setViewVisibility(percentId, View.VISIBLE)
        }
    }
}
