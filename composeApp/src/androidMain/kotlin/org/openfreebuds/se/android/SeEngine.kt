package org.openfreebuds.se.android

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.openfreebuds.se.connection.ConnectionState
import org.openfreebuds.se.connection.SeController
import org.openfreebuds.se.model.SeDevice

/**
 * Process-wide owner of the Bluetooth connection and the periodic battery
 * polling. Created lazily and shared between the foreground service and the
 * activity, so the widget and the permanent notification keep updating in the
 * background even when the UI is closed or recreated.
 */
object SeEngine {

    private const val PREFS_NAME = "se_engine"
    private const val KEY_ADDR = "last_addr"
    private const val KEY_NAME = "last_name"
    private const val SETTINGS_PREFS = "settings"
    private const val SETTINGS_NOTIFICATION = "notification_enabled"

    @Volatile
    private var impl: SeController? = null
    private var scope: CoroutineScope? = null
    private var transport: AndroidBluetoothTransport? = null

    val controller: SeController
        get() = impl ?: error("SeEngine is not started")

    /** The platform transport, exposed so the activity can attach its permission launcher. */
    val platformTransport: AndroidBluetoothTransport?
        get() = transport

    @Synchronized
    fun ensureStarted(context: Context): SeController {
        impl?.let { return it }
        val appCtx = context.applicationContext

        val sc = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val tr = AndroidBluetoothTransport(appCtx, sc)
        val ctrl = SeController(tr, tr, sc)
        tr.onAclConnected = { ctrl.onAclDeviceConnected(it) }
        tr.onAclDisconnected = { ctrl.onAclDeviceDisconnected(it) }
        ctrl.onBatteryChanged = { levels ->
            BatteryWidgetProvider.saveAndUpdate(appCtx, levels)
            BatteryNotification.update(appCtx, levels)
        }

        scope = sc
        transport = tr
        impl = ctrl

        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sc.launch {
            ctrl.state.collect { s ->
                if (s is ConnectionState.Connected) {
                    prefs.edit()
                        .putString(KEY_ADDR, s.device.address)
                        .putString(KEY_NAME, s.device.name)
                        .apply()
                }
            }
        }

        val settings = appCtx.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        val notificationsEnabled = settings.getBoolean(SETTINGS_NOTIFICATION, false)
        val lastAddr = prefs.getString(KEY_ADDR, null)
        val lastName = prefs.getString(KEY_NAME, null) ?: "FreeBuds SE"
        if (notificationsEnabled && lastAddr != null && !tr.needsPermission) {
            tr.refresh()
            ctrl.connectSilent(SeDevice(lastAddr, lastName))
        }
        return ctrl
    }

    fun shutdown() {
        impl?.close()
        scope?.cancel()
        impl = null
        transport = null
        scope = null
    }
}
