package org.openfreebuds.se.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openfreebuds.se.connection.ConnectionState
import org.openfreebuds.se.connection.SeController
import org.openfreebuds.se.ui.components.BatteryCard
import org.openfreebuds.se.ui.components.DevicePanel
import org.openfreebuds.se.ui.components.DoubleTapPanel

@Composable
fun HomeScreen(controller: SeController) {
    val state by controller.state.collectAsState()
    val battery by controller.battery.collectAsState()
    val doubleTap by controller.doubleTap.collectAsState()
    val devices by controller.devices.collectAsState()
    val activeDevice by controller.activeDevice.collectAsState()
    val bluetoothEnabled by controller.bluetoothEnabled.collectAsState()
    val lastError by controller.lastError.collectAsState()
    val reconnecting by controller.reconnecting.collectAsState()
    val notificationsEnabled by controller.notificationsEnabled.collectAsState()

    val connected = state is ConnectionState.Connected
    val connecting = state is ConnectionState.Connecting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "FreeBuds SE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        ConnectionHeader(
            state = state,
            batteryKnown = battery.isKnown,
            reconnecting = reconnecting,
        )

            if (bluetoothEnabled == false && !connected) {
                BluetoothOffCard(onPermission = { controller.requestPermission { } })
            }

            if (lastError != null) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = lastError ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            BatteryCard(battery = battery, connected = connected)
            if (batteryNotificationSupported()) {
                NotificationPanel(
                    enabled = notificationsEnabled,
                    onEnabledChange = controller::setNotificationsEnabled,
                )
            }
            DoubleTapPanel(
                left = doubleTap?.left ?: org.openfreebuds.se.model.TapAction.OFF,
                right = doubleTap?.right ?: org.openfreebuds.se.model.TapAction.OFF,
                enabled = connected && !connecting,
                onLeft = { controller.setDoubleTap(it, doubleTap?.right ?: it) },
                onRight = { controller.setDoubleTap(doubleTap?.left ?: it, it) },
            )

            DevicePanel(
                devices = devices,
                activeDevice = activeDevice,
                connecting = connecting,
                onConnect = controller::connect,
                onDisconnect = controller::disconnect,
                onRefresh = controller::refreshDevices,
            )
    }
}

@Composable
private fun ConnectionHeader(
    state: ConnectionState,
    batteryKnown: Boolean,
    reconnecting: Boolean,
) {
    val connected = state is ConnectionState.Connected
    val connecting = state is ConnectionState.Connecting
    Surface(
        shape = RoundedCornerShape(36.dp),
        color = if (connected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp),
        ) {
            if (connecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = if (connected) Icons.Filled.CheckCircle
                    else if (batteryKnown) Icons.Filled.Bolt
                    else Icons.Filled.BluetoothDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (connected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = when {
                        connected -> "Connected"
                        reconnecting -> "Reconnecting…"
                        state is ConnectionState.Error -> "Connection error"
                        else -> "Not connected"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (connected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun NotificationPanel(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Battery notification",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Show a permanent notification with the battery level of both earbuds and the case.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            androidx.compose.material3.Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun BluetoothOffCard(onPermission: () -> Unit) {    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Bluetooth is disabled",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Enable Bluetooth and open this app again to find your FreeBuds SE.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onPermission) {
                Text("Open settings")
            }
        }
    }
}
