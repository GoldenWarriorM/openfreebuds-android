package org.openfreebuds.se.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openfreebuds.se.model.BatteryLevels
import org.openfreebuds.se.model.DoubleTapConfig
import org.openfreebuds.se.model.SeDevice
import org.openfreebuds.se.model.TapAction
import org.openfreebuds.se.protocol.MbbPackage
import org.openfreebuds.se.protocol.SeCommands

/**
 * Fake [DeviceConnection] + [DeviceProvider] used to preview the app
 * without real hardware. Emulates a FreeBuds SE over time.
 */
class SimulatorConnection(
    private val scope: CoroutineScope,
) : DeviceConnection, DeviceProvider {

    private val fakeDevice = SeDevice("00:AA:BB:CC:DD:EE", "FreeBuds SE")

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _received = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)
    override val received = _received.asSharedFlow()

    private val _devices = MutableStateFlow(listOf(fakeDevice))
    override val devices: StateFlow<List<SeDevice>> = _devices.asStateFlow()

    private val _enabled = MutableStateFlow<Boolean?>(true)
    override val bluetoothEnabled: StateFlow<Boolean?> = _enabled.asStateFlow()

    override val needsPermission: Boolean = false

    private var emulatorJob: Job? = null

    override fun refresh() {
        _devices.value = listOf(fakeDevice.copy(connected = _state.value is ConnectionState.Connected))
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) = onResult(true)

    override fun connect(device: SeDevice) {
        _state.value = ConnectionState.Connecting
        scope.launch {
            delay(1200)
            _state.value = ConnectionState.Connected(device)
            startEmulator()
        }
    }

    override fun disconnect() {
        emulatorJob?.cancel()
        emulatorJob = null
        _state.value = ConnectionState.Disconnected(fakeDevice)
    }

    override fun send(bytes: ByteArray) {
        val pkg = try {
            MbbPackage.fromBytes(bytes, verifyChecksum = true)
        } catch (_: IllegalArgumentException) {
            return
        }
        when (pkg.serviceId to pkg.commandId) {
            SeCommands.BATTERY_READ_SERVICE to SeCommands.BATTERY_READ_COMMAND ->
                reply(batteryResponse())
            SeCommands.INFO_SERVICE to SeCommands.INFO_COMMAND ->
                reply(infoResponse())
            SeCommands.DUAL_TAP_READ_SERVICE to SeCommands.DUAL_TAP_READ_COMMAND ->
                reply(doubleTapResponse())
        }
    }

    override fun close() {
        emulatorJob?.cancel()
    }

    private fun startEmulator() {
        emulatorJob?.cancel()
        emulatorJob = scope.launch {
            delay(500)
            while (true) {
                reply(batteryResponse())
                delay(15_000)
            }
        }
    }

    private fun batteryResponse(): MbbPackage {
        val left = (45..100).random()
        val right = (45..100).random()
        val case = (30..100).random()
        return MbbPackage(1, SeCommands.BATTERY_NOTIFY_COMMAND).apply {
            params[1] = byteArrayOf(minOf(left, right).toByte())
            params[2] = byteArrayOf(left.toByte(), right.toByte(), case.toByte())
            params[3] = byteArrayOf(0, 0, 0)
        }
    }

    private fun infoResponse(): MbbPackage =
        MbbPackage(1, SeCommands.INFO_COMMAND).apply {
            params[15] = "FreeBuds SE".toByteArray()
            params[7] = "1.9.0.198".toByteArray()
        }

    private fun doubleTapResponse(): MbbPackage =
        MbbPackage(1, SeCommands.DUAL_TAP_READ_COMMAND).apply {
            params[1] = byteArrayOf(1)
            params[2] = byteArrayOf(2)
        }

    private fun reply(pkg: MbbPackage) {
        _received.tryEmit(pkg.toBytes())
    }
}
