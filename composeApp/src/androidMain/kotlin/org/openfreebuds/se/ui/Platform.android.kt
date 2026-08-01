package org.openfreebuds.se.ui

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.openfreebuds.se.android.BatteryNotification
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

internal actual fun platformBatteryNotificationSupported(): Boolean = true

internal actual fun platformSetBatteryNotificationEnabled(enabled: Boolean) {
    val context = appContext ?: return
    if (enabled) {
        BatteryNotification.show(context, BatteryLevels.Unknown)
    } else {
        BatteryNotification.hide(context)
    }
}

internal actual fun platformUpdateBatteryNotification(battery: BatteryLevels) {
    val context = appContext ?: return
    BatteryNotification.update(context, battery)
}
