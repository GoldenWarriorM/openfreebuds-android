package org.openfreebuds.se.ui

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.openfreebuds.se.android.BatteryNotification
import org.openfreebuds.se.android.BatteryStatusService
import org.openfreebuds.se.model.BatteryLevels

internal var appContext: Context? = null

fun initPlatform(context: Context) {
    appContext = context.applicationContext
}

@Composable
actual fun platformColorScheme(isDark: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDark) DarkColorScheme else LightColorScheme
    }
}

/**
 * Reads the system Material You accent1 tone as used by the home widget.
 * Falls back to fixed accent colors on API < 31.
 */
@Composable
actual fun platformAccentTone(tone: Int): Color {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        @Suppress("DEPRECATION")
        val argb = context.getColor(
            when (tone) {
                10 -> android.R.color.system_accent1_10
                50 -> android.R.color.system_accent1_50
                100 -> android.R.color.system_accent1_100
                200 -> android.R.color.system_accent1_200
                300 -> android.R.color.system_accent1_300
                500 -> android.R.color.system_accent1_500
                700 -> android.R.color.system_accent1_700
                800 -> android.R.color.system_accent1_800
                else -> android.R.color.system_accent1_500
            },
        )
        return Color(argb)
    }
    return when (tone) {
        10, 100 -> Color(0xFF9CF2E3)
        200 -> Color(0xFFCCE8E2)
        700 -> Color(0xFF005048)
        800 -> Color(0xFF9CF2E3)
        else -> Color(0xFF006A60)
    }
}

internal actual fun platformBatteryNotificationSupported(): Boolean = true

internal actual fun platformSetBatteryNotificationEnabled(enabled: Boolean) {
    val context = appContext ?: return
    if (enabled) {
        BatteryStatusService.start(context)
    } else {
        BatteryStatusService.stop(context)
    }
}

internal actual fun platformUpdateBatteryNotification(battery: BatteryLevels) {
    val context = appContext ?: return
    BatteryNotification.update(context, battery)
}
