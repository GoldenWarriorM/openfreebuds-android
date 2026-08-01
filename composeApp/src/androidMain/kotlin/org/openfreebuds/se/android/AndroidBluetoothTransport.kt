package org.openfreebuds.se.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import org.openfreebuds.se.connection.ConnectionState
import org.openfreebuds.se.connection.DeviceConnection
import org.openfreebuds.se.connection.DeviceProvider
import org.openfreebuds.se.model.SeDevice

/**
 * Android Bluetooth Classic (RFCOMM/SPP) transport for the FreeBuds SE.
 * Uses the well known serial port UUID, with an insecure socket fallback.
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothTransport(
    private val appContext: Context,
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

    private var socket: BluetoothSocket? = null
    private var recvJob: kotlinx.coroutines.Job? = null

    var permissionLauncher: ActivityResultLauncher<String>? = null
    private var pendingDevice: SeDevice? = null

    override val needsPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            appContext, "android.permission.BLUETOOTH_CONNECT",
        ) != PackageManager.PERMISSION_GRANTED

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                _enabled.value = isBluetoothEnabled()
            }
        }
    }

    init {
        appContext.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
        )
        _enabled.value = isBluetoothEnabled()
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        val launcher = permissionLauncher ?: return onResult(needsPermission.not())
        launcher.launch("android.permission.BLUETOOTH_CONNECT")
        // best effort: the activity handles the callback separately
    }

    private fun isBluetoothEnabled(): Boolean? {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
        return adapter.isEnabled
    }

    @SuppressLint("MissingPermission")
    override fun refresh() {
        if (needsPermission) {
            _devices.value = emptyList()
            return
        }
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: run {
            _devices.value = emptyList()
            return
        }
        if (!adapter.isEnabled) {
            _devices.value = emptyList()
            return
        }
        val found = adapter.bondedDevices
            .filter { it.type != BluetoothDevice.DEVICE_TYPE_LE }
            .mapNotNull { device ->
                if (!isLikelyFreeBudsSe(device.name)) return@mapNotNull null
                SeDevice(
                    address = device.address,
                    name = device.name ?: "FreeBuds SE",
                )
            }
            .sortedBy { it.name }
        _devices.value = found
    }

    override fun connect(device: SeDevice) {
        android.util.Log.d("FreeBudsSE", "connect() requested ${device.address}")
        if (needsPermission) {
            pendingDevice = device
            requestPermission { }
            return
        }
        scope.launch { openConnection(device) }
    }

    /** Called by the activity after the permission dialog result. */
    fun onPermissionResult(granted: Boolean) {
        val device = pendingDevice ?: return
        pendingDevice = null
        if (granted) {
            scope.launch { openConnection(device) }
        }
    }

    override fun disconnect() {
        android.util.Log.d("FreeBudsSE", "disconnect() called")
        recvJob?.cancel()
        recvJob = null
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        _state.value = ConnectionState.Disconnected(null)
    }

    override fun send(bytes: ByteArray) {
        val sock = socket ?: return
        try {
            sock.outputStream.write(bytes)
            sock.outputStream.flush()
        } catch (_: Exception) {
            disconnect()
        }
    }

    override fun close() {
        disconnect()
        try {
            appContext.unregisterReceiver(bluetoothStateReceiver)
        } catch (_: Exception) {
        }
    }

    private suspend fun openConnection(device: SeDevice) {
        _state.value = ConnectionState.Connecting
        android.util.Log.d("FreeBudsSE", "openConnection: Connecting")
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            _state.value = ConnectionState.Error("Bluetooth is not available")
            return
        }
        val remote = adapter.getRemoteDevice(device.address)

        val sock = connectSocket(remote)
        if (sock == null) {
            _state.value = ConnectionState.Error(
                "Could not connect to ${device.name}. Try again.",
            )
            android.util.Log.d("FreeBudsSE", "openConnection: FAILED")
            return
        }

        socket = sock
        _state.value = ConnectionState.Connected(device)
        android.util.Log.d("FreeBudsSE", "openConnection: Connected OK")
        recvJob = scope.launch {
            val input = sock.inputStream
            val buffer = ByteArray(4096)
            while (isActive) {
                val n = try {
                    withContext(Dispatchers.IO) { input.read(buffer) }
                } catch (_: Exception) {
                    -1
                }
                if (n <= 0) {
                    android.util.Log.d("FreeBudsSE", "reader: socket closed n=$n")
                    break
                }
                _received.tryEmit(buffer.copyOf(n))
            }
            disconnect()
        }
    }

    private fun connectSocket(remote: BluetoothDevice): BluetoothSocket? {
        val attempts = listOf(
            { remote.createRfcommSocketToServiceRecord(sppUuid) },
            { remote.createInsecureRfcommSocketToServiceRecord(sppUuid) },
            { remote.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(remote, 16) as BluetoothSocket },
        )
        for (factory in attempts) {
            val sock = try {
                factory()
            } catch (_: Exception) {
                continue
            }
            try {
                sock.connect()
                return sock
            } catch (_: Exception) {
                try {
                    sock.close()
                } catch (_: Exception) {
                }
            }
        }
        return null
    }

    companion object {
        private val sppUuid = java.util.UUID.fromString(
            "00001101-0000-1000-8000-00805f9b34fb",
        )

        private fun isLikelyFreeBudsSe(name: String?): Boolean {
            val n = name?.lowercase() ?: return false
            return "freebuds se" in n
        }
    }
}
