package org.openfreebuds.se.android

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openfreebuds.se.connection.ConnectionState
import org.openfreebuds.se.connection.SeController
import org.openfreebuds.se.ui.App
import org.openfreebuds.se.ui.initPlatform

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var controller: SeController
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("settings", MODE_PRIVATE)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        SeEngine.platformTransport?.onPermissionResult(granted)
        if (granted) refreshAndMaybeConnect()
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        SeEngine.platformTransport?.onBluetoothEnableResult()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && controller.notificationsEnabled.value) {
            controller.setNotificationsEnabled(false)
            controller.setNotificationsEnabled(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initPlatform(applicationContext)

        controller = SeEngine.ensureStarted(applicationContext)
        SeEngine.platformTransport?.permissionLauncher = permissionLauncher
        SeEngine.platformTransport?.enableBluetoothLauncher = enableBluetoothLauncher

        if (prefs.getBoolean("notification_enabled", false)) {
            controller.setNotificationsEnabled(true)
        }

        scope.launch {
            controller.notificationsEnabled.collect { enabled ->
                prefs.edit().putBoolean("notification_enabled", enabled).apply()
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        applicationContext,
                        "android.permission.POST_NOTIFICATIONS",
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                }
            }
        }

        scope.launch {
            if (SeEngine.platformTransport?.needsPermission == true) {
                SeEngine.platformTransport?.requestPermission { granted ->
                    if (granted) refreshAndMaybeConnect()
                }
            } else {
                refreshAndMaybeConnect()
            }
        }

        setContent {
            App(controller)
        }
    }

    private fun refreshAndMaybeConnect() {
        scope.launch {
            var attempts = 0
            while (
                attempts < 10 &&
                controller.state.value !is ConnectionState.Connected &&
                controller.state.value !is ConnectionState.Connecting
            ) {
                controller.refreshDevices()
                val devices = SeEngine.platformTransport?.devices?.value.orEmpty()
                if (devices.isNotEmpty()) {
                    controller.connectSilent(devices.first())
                    return@launch
                }
                delay(700)
                attempts++
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
