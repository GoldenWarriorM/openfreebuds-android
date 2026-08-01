package org.openfreebuds.se.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import org.openfreebuds.se.model.BatteryLevels

/**
 * Returns a platform color scheme. On Android this is the dynamic (wallpaper
 * based) Material You scheme; on desktop a fixed baseline is used.
 */
@Composable
expect fun platformColorScheme(isDark: Boolean): ColorScheme

/** Whether the platform can show an ongoing battery notification. */
fun batteryNotificationSupported(): Boolean = platformBatteryNotificationSupported()

/** Enables/disables the persistent battery notification. */
fun setBatteryNotificationEnabled(enabled: Boolean) = platformSetBatteryNotificationEnabled(enabled)

/** Updates the persistent battery notification, if enabled. */
fun updateBatteryNotification(battery: BatteryLevels) = platformUpdateBatteryNotification(battery)

internal expect fun platformBatteryNotificationSupported(): Boolean
internal expect fun platformSetBatteryNotificationEnabled(enabled: Boolean)
internal expect fun platformUpdateBatteryNotification(battery: BatteryLevels)
