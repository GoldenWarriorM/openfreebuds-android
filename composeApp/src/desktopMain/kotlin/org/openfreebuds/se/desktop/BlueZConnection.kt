package org.openfreebuds.se.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.Variant
import org.openfreebuds.se.connection.ConnectionState
import org.openfreebuds.se.connection.DeviceConnection
import org.openfreebuds.se.connection.DeviceProvider
import org.openfreebuds.se.model.SeDevice

/**
 * Linux transport backed by BlueZ.
 *
 * Discovery uses the `org.bluez` D-Bus object manager. Connecting opens a
 * raw `AF_BLUETOOTH` RFCOMM socket on channel 16 directly to the earbuds,
 * same approach as OpenFreebuds — no BlueZ profile registration needed.
 */
class BlueZConnection(
    private val scope: CoroutineScope,
) : DeviceConnection, DeviceProvider {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _received = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val received: SharedFlow<ByteArray> = _received.asSharedFlow()

    private val _devices = MutableStateFlow<List<SeDevice>>(emptyList())
    override val devices: StateFlow<List<SeDevice>> = _devices.asStateFlow()

    private val _enabled = MutableStateFlow<Boolean?>(null)
    override val bluetoothEnabled: StateFlow<Boolean?> = _enabled.asStateFlow()

    override val needsPermission: Boolean = false

    private var dbus: DBusConnection? = null
    private var rfcomm: RfcommSocket? = null
    private var readerJob: kotlinx.coroutines.Job? = null

    override fun refresh() {
        scope.launch {
            try {
                val connection = getDbus()
                val powered = adapterPowered(connection)
                _enabled.value = powered
                if (powered) {
                    _devices.value = findFreeBudsSeDevices(connection)
                }
            } catch (_: Exception) {
                _enabled.value = false
            }
        }
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) = onResult(true)

    override fun connect(device: SeDevice) {
        scope.launch {
            _state.value = ConnectionState.Connecting
            try {
                val socket = withContext(Dispatchers.IO) {
                    RfcommSocket(device.address)
                }
                rfcomm = socket
                _state.value = ConnectionState.Connected(device)
                readerJob = scope.launch {
                    val buffer = ByteArray(4096)
                    while (isActive) {
                        val n = withContext(Dispatchers.IO) {
                            socket.read(buffer, 0, buffer.size)
                        }
                        if (n <= 0) break
                        _received.tryEmit(buffer.copyOf(n))
                    }
                    disconnect()
                }
            } catch (e: Exception) {
                _state.value = ConnectionState.Error(
                    "Connection failed: ${e.message ?: "unknown error"}",
                )
            }
        }
    }

    override fun disconnect() {
        readerJob?.cancel()
        readerJob = null
        try {
            rfcomm?.close()
        } catch (_: Exception) {
        }
        rfcomm = null
        _state.value = ConnectionState.Disconnected(null)
    }

    override fun send(bytes: ByteArray) {
        val socket = rfcomm ?: return
        try {
            socket.write(bytes)
        } catch (_: Exception) {
            disconnect()
        }
    }

    override fun close() {
        disconnect()
        try {
            dbus?.close()
        } catch (_: Exception) {
        }
        dbus = null
    }

    private suspend fun getDbus(): DBusConnection {
        dbus?.let { return it }
        val connection = withContext(Dispatchers.IO) {
            DBusConnectionBuilder.forSystemBus().build()
        }
        dbus = connection
        return connection
    }

    private fun adapterPowered(connection: DBusConnection): Boolean {
        val objects = managedObjects(connection)
        for ((_, interfaces) in objects) {
            val adapter = interfaces["org.bluez.Adapter1"] ?: continue
            val powered = adapter["Powered"]?.value as? Boolean ?: false
            return powered
        }
        return false
    }

    private fun findFreeBudsSeDevices(connection: DBusConnection): List<SeDevice> {
        val objects = managedObjects(connection)
        val result = ArrayList<SeDevice>()
        for ((_, interfaces) in objects) {
            val dev = interfaces["org.bluez.Device1"] ?: continue
            val paired = dev["Paired"]?.value as? Boolean ?: false
            if (!paired) continue
            val name = dev["Alias"]?.value as? String
                ?: dev["Name"]?.value as? String
                ?: continue
            if (!isLikelyFreeBudsSe(name)) continue
            val address = dev["Address"]?.value as? String ?: continue
            val connected = dev["Connected"]?.value as? Boolean ?: false
            result.add(SeDevice(address = address, name = name, connected = connected))
        }
        return result.sortedBy { it.name }
    }

    private fun managedObjects(connection: DBusConnection):
        Map<DBusPath, Map<String, Map<String, Variant<*>>>> {
        val om = connection.getRemoteObject(
            BLUEZ_BUS, "/", ObjectManager::class.java, true,
        )
        return om.getManagedObjects()
    }

    @DBusInterfaceName("org.freedesktop.DBus.ObjectManager")
    private interface ObjectManager : DBusInterface {
        @DBusMemberName("GetManagedObjects")
        fun getManagedObjects(): Map<DBusPath, Map<String, Map<String, Variant<*>>>>
    }

    companion object {
        private const val BLUEZ_BUS = "org.bluez"

        private fun isLikelyFreeBudsSe(name: String): Boolean =
            "freebuds se" in name.lowercase()
    }
}
