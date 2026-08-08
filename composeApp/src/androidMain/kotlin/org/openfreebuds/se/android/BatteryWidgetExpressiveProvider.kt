package org.openfreebuds.se.android

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RemoteViews
import org.openfreebuds.se.R
import kotlin.math.roundToInt

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
            appWidgetManager.updateAppWidget(
                id,
                buildViews(context, data, storedShowLabels(context, id)
                    ?: showLabels(appWidgetManager.getAppWidgetOptions(id))),
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        val data = BatteryWidgetProvider.WidgetData.load(context)
        storeShowLabels(context, appWidgetId, showLabels(newOptions))
        appWidgetManager.updateAppWidget(
            appWidgetId,
            buildViews(context, data, showLabels(newOptions)),
        )
    }

    companion object {
        private const val TAG = "ExpressiveWidget"

        /** Progress bar tween length, in milliseconds. */
        private const val ANIM_DURATION_MS = 600L

        /**
         * Per-widget animated state. [Float.NaN] fields mean "no known level",
         * charging flags are carried along so the Bolt icon stays correct.
         */
        private data class AnimatedLevels(
            val left: Float,
            val right: Float,
            val case: Float,
            val chargingLeft: Boolean,
            val chargingRight: Boolean,
            val chargingCase: Boolean,
        )

        /** Last value actually pushed to the launcher, per widget instance. */
        private val rendered = HashMap<Int, AnimatedLevels>()

        /** In-flight tweens, per widget instance. */
        private val tweens = HashMap<Int, ValueAnimator>()

        private const val LABEL_PREFS = "battery_widget_options"
        private const val LABEL_KEY = "show_labels_"

        /**
         * Labels state is persisted so that a stale [onUpdate] delivered after
         * a resize (with outdated options) never brings the labels back for a
         * frame; the latest decision from [onAppWidgetOptionsChanged] wins.
         */
        private fun labelsPref(context: Context): SharedPreferences =
            context.getSharedPreferences(LABEL_PREFS, Context.MODE_PRIVATE)

        private fun storedShowLabels(context: Context, id: Int): Boolean? {
            val prefs = labelsPref(context)
            return if (prefs.contains(LABEL_KEY + id)) {
                prefs.getBoolean(LABEL_KEY + id, false)
            } else {
                null
            }
        }

        private fun storeShowLabels(context: Context, id: Int, show: Boolean) {
            labelsPref(context).edit().putBoolean(LABEL_KEY + id, show).apply()
        }

        fun updateAll(context: Context, data: BatteryWidgetProvider.WidgetData) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, BatteryWidgetExpressiveProvider::class.java),
            )
            val target = AnimatedLevels(
                left = data.left?.toFloat() ?: Float.NaN,
                right = data.right?.toFloat() ?: Float.NaN,
                case = data.case?.toFloat() ?: Float.NaN,
                chargingLeft = data.chargingLeft,
                chargingRight = data.chargingRight,
                chargingCase = data.chargingCase,
            )
            ids.forEach { id ->
                animateTo(context, manager, id, target)
            }
        }

        /**
         * Raises the widget gradually toward [target]. RemoteViews cannot
         * animate by themselves, so we drive a short frame-by-frame
         * [ValueAnimator], pushing a freshly built [RemoteViews] every frame.
         * Battery changes are rare (polled every ~30s), so this is cheap.
         */
        private fun animateTo(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            target: AnimatedLevels,
        ) {
            val from = rendered[id] ?: target
            tweens.remove(id)
                ?.let { anim -> anim.cancel() }
            if (sameLevels(from, target)) {
                push(context, manager, id, target)
                return
            }
            val anim = ValueAnimator.ofFloat(0f, 1f)
            anim.duration = ANIM_DURATION_MS
            anim.interpolator = AccelerateDecelerateInterpolator()
            anim.addUpdateListener { value ->
                val t = value.animatedValue as Float
                push(context, manager, id, AnimatedLevels(
                    left = lerp(from.left, target.left, t),
                    right = lerp(from.right, target.right, t),
                    case = lerp(from.case, target.case, t),
                    chargingLeft = target.chargingLeft,
                    chargingRight = target.chargingRight,
                    chargingCase = target.chargingCase,
                ))
            }
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) = Unit
                override fun onAnimationRepeat(animation: Animator) = Unit
                override fun onAnimationEnd(animation: Animator) {
                    if (tweens[id] === anim) {
                        push(context, manager, id, target)
                    }
                }
                override fun onAnimationCancel(animation: Animator) = Unit
            })
            tweens[id] = anim
            anim.start()
        }

        private fun push(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            levels: AnimatedLevels,
        ) {
            rendered[id] = levels
            val showLabel = storedShowLabels(context, id)
                ?: showLabels(manager.getAppWidgetOptions(id))
            val data = BatteryWidgetProvider.WidgetData(
                left = levels.left.snapInt(),
                right = levels.right.snapInt(),
                case = levels.case.snapInt(),
                chargingLeft = levels.chargingLeft,
                chargingRight = levels.chargingRight,
                chargingCase = levels.chargingCase,
            )
            manager.updateAppWidget(id, buildViews(context, data, showLabel))
        }

        private fun sameLevels(a: AnimatedLevels, b: AnimatedLevels): Boolean =
            a.left == b.left && a.right == b.right && a.case == b.case

        private fun lerp(from: Float, to: Float, t: Float): Float {
            if (from.isNaN()) return to
            if (to.isNaN()) return from
            return from + (to - from) * t
        }

        private fun Float.snapInt(): Int? =
            if (isNaN()) null else roundToInt().coerceIn(0, 100)

        /**
         * Labels (Left/Right/Case) are shown only when the widget is at least
         * three cells wide (~216dp). A strict ">=3 cells" boundary (not a
         * rounded cell count) means the label pops in only once the widget is
         * fully three cells, and disappears the moment it drops to any smaller
         * size — no flicker while resizing.
         */
        private fun showLabels(options: android.os.Bundle): Boolean {
            val minWidthDp = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                0,
            ).toFloat()
            return minWidthDp >= 72f * 3f
        }

        private fun buildViews(
            context: Context,
            data: BatteryWidgetProvider.WidgetData,
            showLabels: Boolean,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_battery_expressive)
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            configureCell(
                views,
                labelId = R.id.fb_left_label,
                progressId = R.id.fb_left_progress,
                percentId = R.id.fb_left_percent,
                chargingId = R.id.fb_left_charging,
                level = data.left,
                charging = data.chargingLeft,
                showLabel = showLabels,
            )
            configureCell(
                views,
                labelId = R.id.fb_right_label,
                progressId = R.id.fb_right_progress,
                percentId = R.id.fb_right_percent,
                chargingId = R.id.fb_right_charging,
                level = data.right,
                charging = data.chargingRight,
                showLabel = showLabels,
            )
            configureCell(
                views,
                labelId = R.id.fb_case_label,
                progressId = R.id.fb_case_progress,
                percentId = R.id.fb_case_percent,
                chargingId = R.id.fb_case_charging,
                level = data.case,
                charging = data.chargingCase,
                showLabel = showLabels,
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
            labelId: Int,
            progressId: Int,
            percentId: Int,
            chargingId: Int,
            level: Int?,
            charging: Boolean,
            showLabel: Boolean,
        ) {
            views.setViewVisibility(labelId, if (showLabel) View.VISIBLE else View.GONE)
            views.setViewVisibility(
                chargingId,
                if (charging && level != null) View.VISIBLE else View.GONE,
            )
            if (level == null) {
                views.setTextViewText(percentId, "—")
                views.setProgressBar(progressId, 100, 0, false)
            } else {
                views.setTextViewText(percentId, "$level%")
                views.setProgressBar(progressId, 100, level.coerceIn(0, 100), false)
            }
            views.setViewVisibility(percentId, View.VISIBLE)
        }
    }
}