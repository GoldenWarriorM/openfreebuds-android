package org.openfreebuds.se.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.openfreebuds.se.model.SeDevice

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val device: SeDevice) : ConnectionState
    data class Disconnected(val device: SeDevice?) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

/**
 * Platform-specific byte transport to the headphones.
 * Produces raw bytes received from the device and accepts raw bytes to send.
 */
interface DeviceConnection {
    val state: StateFlow<ConnectionState>
    val received: Flow<ByteArray>

    fun connect(device: SeDevice)
    fun disconnect()
    fun send(bytes: ByteArray)
    fun close()

    /**
     * Whether the given device is currently linked at the Bluetooth ACL layer
     * (i.e. physically connected to the phone). Used to avoid pointless SPP
     * reconnect attempts while the device is out of range. Defaults to true so
     * platforms without ACL awareness keep retrying.
     */
    fun isAclConnected(address: String): Boolean = true
}

/** Platform-specific device discovery. */
interface DeviceProvider {
    val devices: StateFlow<List<SeDevice>>
    val bluetoothEnabled: StateFlow<Boolean?>
    val needsPermission: Boolean

    fun refresh()
    fun requestPermission(onResult: (Boolean) -> Unit)
}
