package org.openfreebuds.se.android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import org.openfreebuds.se.R

/**
 * Second home screen widget: three rounded "pill" boxes (left / right / case)
 * each filled with a horizontal progress bar and showing the percentage.
 */
class BatteryWidgetExpressiveProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val data = BatteryWidgetProvider.WidgetData.load(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, data))
        }
    }

    companion object {
        fun updateAll(context: Context, data: BatteryWidgetProvider.WidgetData) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, BatteryWidgetExpressiveProvider::class.java),
            )
            ids.forEach { id ->
                manager.updateAppWidget(id, buildViews(context, data))
            }
        }

        private fun buildViews(context: Context, data: BatteryWidgetProvider.WidgetData): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_battery_expressive)
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            configureCell(
                views,
                progressId = R.id.fb_left_progress,
                percentId = R.id.fb_left_percent,
                level = data.left,
                charging = data.chargingLeft,
            )
            configureCell(
                views,
                progressId = R.id.fb_right_progress,
                percentId = R.id.fb_right_percent,
                level = data.right,
                charging = data.chargingRight,
            )
            configureCell(
                views,
                progressId = R.id.fb_case_progress,
                percentId = R.id.fb_case_percent,
                level = data.case,
                charging = data.chargingCase,
            )
            return views
        }

        private fun openAppIntent(context: Context): PendingIntent {
            return PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun configureCell(
            views: RemoteViews,
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