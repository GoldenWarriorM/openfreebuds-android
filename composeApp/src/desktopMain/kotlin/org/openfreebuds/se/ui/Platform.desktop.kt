package org.openfreebuds.se.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.openfreebuds.se.model.BatteryLevels

@Composable
actual fun platformColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) DarkColorScheme else LightColorScheme

@Composable
actual fun platformAccentTone(tone: Int): Color = when (tone) {
    100 -> Color(0xFF9CF2E3)
    200 -> Color(0xFFCCE8E2)
    700 -> Color(0xFF005048)
    else -> Color(0xFF006A60)
}

internal actual fun platformBatteryNotificationSupported(): Boolean = false

internal actual fun platformSetBatteryNotificationEnabled(enabled: Boolean) = Unit

internal actual fun platformUpdateBatteryNotification(battery: BatteryLevels) = Unit
