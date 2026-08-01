package org.openfreebuds.se.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.openfreebuds.se.model.BatteryLevels
import org.openfreebuds.se.model.DeviceInfo
import org.openfreebuds.se.model.DoubleTapConfig
import org.openfreebuds.se.model.SeDevice
import org.openfreebuds.se.model.TapAction
import org.openfreebuds.se.protocol.MbbPackage
import org.openfreebuds.se.protocol.MbbStreamParser
import org.openfreebuds.se.protocol.SeCommands
import org.openfreebuds.se.ui.setBatteryNotificationEnabled
import org.openfreebuds.se.ui.updateBatteryNotification

/**
 * Shared headphone state and protocol logic, independent of the platform
 * transport. Parses MBB packages, keeps battery / device info / gesture
 * config state and periodically re-queries missing data while connected.
 */
class SeController(
    private val connection: DeviceConnection,
    private val provider: DeviceProvider,
    private val scope: CoroutineScope,
) {
    private val parser = MbbStreamParser()

    private val _battery = MutableStateFlow(BatteryLevels.Unknown)
    val battery: StateFlow<BatteryLevels> = _battery.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _doubleTap = MutableStateFlow<DoubleTapConfig?>(null)
    val doubleTap: StateFlow<DoubleTapConfig?> = _doubleTap.asStateFlow()

    private val _activeDevice = MutableStateFlow<SeDevice?>(null)
    val activeDevice: StateFlow<SeDevice?> = _activeDevice.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val connectionState: StateFlow<ConnectionState> = connection.state
    val state: StateFlow<ConnectionState> = connectionState

    val devices: StateFlow<List<SeDevice>> = provider.devices
    val bluetoothEnabled: StateFlow<Boolean?> = provider.bluetoothEnabled
    val needsPermission: Boolean get() = provider.needsPermission

    /** Called whenever new battery data arrives (used to push widget updates). */
    var onBatteryChanged: ((BatteryLevels) -> Unit)? = null

    private var watchdogJob: Job? = null
    private var reconnectJob: Job? = null
    private var lastDevice: SeDevice? = null
    private var reconnectAttempts = 0
    private var userDisconnected = false
    private var connectedSince = 0L
    private val _reconnecting = MutableStateFlow(false)
    val reconnecting: StateFlow<Boolean> = _reconnecting.asStateFlow()

    init {
        scope.launch {
            connection.received.collect { chunk ->
                for (pkg in parser.push(chunk)) handlePackage(pkg)
            }
        }

        scope.launch {
            connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        connectedSince = System.currentTimeMillis()
                        _reconnecting.value = false
                        reconnectJob?.cancel()
                        reconnectJob = null
                        lastDevice = state.device
                        _activeDevice.value = state.device
                        _lastError.value = null
                        requestInit()
                        startWatchdog()
                    }
                    is ConnectionState.Disconnected -> {
                        watchdogJob?.cancel()
                        watchdogJob = null
                        _activeDevice.value = null
                        onConnectionLost()
                    }
                    is ConnectionState.Error -> {
                        _lastError.value = state.message
                        _activeDevice.value = null
                        onConnectionLost()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun refreshDevices() = provider.refresh()

    fun connect(device: SeDevice) {
        userDisconnected = false
        reconnectJob?.cancel()
        reconnectJob = null
        _reconnecting.value = false
        watchdogJob?.cancel()
        connection.connect(device)
    }

    fun disconnect() {
        userDisconnected = true
        reconnectJob?.cancel()
        reconnectJob = null
        _reconnecting.value = false
        watchdogJob?.cancel()
        connection.disconnect()
    }

    private fun onConnectionLost() {
        val now = System.currentTimeMillis()
        val wasStable = connectedSince != 0L && now - connectedSince >= STABLE_WINDOW_MS
        connectedSince = 0L
        if (wasStable) reconnectAttempts = 0
        if (userDisconnected || lastDevice == null) return
        _reconnecting.value = reconnectAttempts < MAX_RECONNECTS
        scheduleReconnect()
    }

    /** Reconnects automatically after an unexpected drop, up to a few tries. */
    private fun scheduleReconnect() {
        if (userDisconnected || lastDevice == null || reconnectAttempts >= MAX_RECONNECTS) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            reconnectAttempts++
            connection.connect(lastDevice!!)
        }
    }

    fun requestPermission(onResult: (Boolean) -> Unit) =
        provider.requestPermission(onResult)

    fun setDoubleTap(left: TapAction, right: TapAction) {
        connection.send(SeCommands.doubleTapWrite(left, right).toBytes())
        connection.send(SeCommands.doubleTapRead().toBytes())
    }

    private fun handlePackage(pkg: MbbPackage) {
        if (pkg.serviceId == SeCommands.LOG_SERVICE && pkg.commandId == SeCommands.LOG_COMMAND) return
        when (pkg.serviceId to pkg.commandId) {
            SeCommands.BATTERY_READ_SERVICE to SeCommands.BATTERY_READ_COMMAND,
            SeCommands.BATTERY_NOTIFY_SERVICE to SeCommands.BATTERY_NOTIFY_COMMAND,
            -> {
                _battery.value = SeCommands.parseBattery(pkg)
                onBatteryChanged?.invoke(_battery.value)
            }
            SeCommands.INFO_SERVICE to SeCommands.INFO_COMMAND -> {
                _deviceInfo.value = SeCommands.parseInfo(pkg)
            }
            SeCommands.DUAL_TAP_READ_SERVICE to SeCommands.DUAL_TAP_READ_COMMAND -> {
                SeCommands.parseDoubleTap(pkg)?.let { (left, right) ->
                    _doubleTap.value = DoubleTapConfig(left, right)
                }
            }
        }
    }

    private fun requestInit() {
        connection.send(SeCommands.batteryRead().toBytes())
        connection.send(SeCommands.infoRead().toBytes())
        connection.send(SeCommands.doubleTapRead().toBytes())
    }

    /** Re-queries missing data every few seconds, like FreeBuddy does. */
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(3000)
                val hasBattery = _battery.value.isKnown
                val hasInfo = _deviceInfo.value != null
                val hasDoubleTap = _doubleTap.value != null
                if (!hasBattery) connection.send(SeCommands.batteryRead().toBytes())
                if (!hasInfo) connection.send(SeCommands.infoRead().toBytes())
                if (!hasDoubleTap) connection.send(SeCommands.doubleTapRead().toBytes())
                if (hasBattery && hasInfo && hasDoubleTap) break
            }
        }
    }

    fun close() {
        userDisconnected = true
        watchdogJob?.cancel()
        reconnectJob?.cancel()
        connection.close()
    }

    fun updateBatteryManually(levels: BatteryLevels) {
        _battery.update { levels }
    }

    /** Enables or disables the persistent battery notification, if supported. */
    fun setNotificationsEnabled(enabled: Boolean) {
        if (_notificationsEnabled.value == enabled) return
        _notificationsEnabled.value = enabled
        if (enabled) {
            setBatteryNotificationEnabled(true)
            updateBatteryNotification(_battery.value)
        } else {
            setBatteryNotificationEnabled(false)
        }
    }

    companion object {
        private const val MAX_RECONNECTS = 3
        private const val STABLE_WINDOW_MS = 15_000L
    }
}
