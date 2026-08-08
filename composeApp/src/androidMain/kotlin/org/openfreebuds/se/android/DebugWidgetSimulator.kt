package org.openfreebuds.se.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.openfreebuds.se.model.BatteryLevels

/**
 * Debug-only hook: simulates incoming battery data to exercise the widget
 * update path AND the in-app battery card without a real device.
 *
 *   adb shell am broadcast -a org.openfreebuds.simulate_battery \
 *       --ei left 45 --ei right 30 --ei case 20
 *
 * Any omitted extra keeps its previously persisted value. Pass -1 for a side
 * to mark it as "missing/offline" (renders the grey offline pill).
 */
class DebugWidgetSimulator : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prev = BatteryWidgetProvider.WidgetData.load(context)
        val left = intent.getIntExtra("left", prev.left ?: -1)
        val right = intent.getIntExtra("right", prev.right ?: -1)
        val case = intent.getIntExtra("case", prev.case ?: -1)
        val chargingLeft = intent.getBooleanExtra("charging_left", prev.chargingLeft)
        val chargingRight = intent.getBooleanExtra("charging_right", prev.chargingRight)
        val chargingCase = intent.getBooleanExtra("charging_case", prev.chargingCase)

        val levels = BatteryLevels(
            left = left.takeIf { it >= 0 },
            right = right.takeIf { it >= 0 },
            case = case.takeIf { it >= 0 },
            chargingLeft = chargingLeft,
            chargingRight = chargingRight,
            chargingCase = chargingCase,
            missingLeft = left < 0,
            missingRight = right < 0,
            missingCase = case < 0,
        )

        BatteryWidgetProvider.saveAndUpdate(context, levels)

        // Also push into the running app's controller so the in-app battery
        // card updates (and exercises the offline grey styles) on the phone.
        runCatching { SeEngine.controller.updateBatteryManually(levels) }
    }
}