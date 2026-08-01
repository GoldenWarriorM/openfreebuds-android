package org.openfreebuds.se.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import org.openfreebuds.se.model.BatteryLevels

@Composable
actual fun platformColorScheme(isDark: Boolean): ColorScheme =
    if (isDark) DarkColorScheme else LightColorScheme

internal actual fun platformBatteryNotificationSupported(): Boolean = false

internal actual fun platformSetBatteryNotificationEnabled(enabled: Boolean) = Unit

internal actual fun platformUpdateBatteryNotification(battery: BatteryLevels) = Unit
