package org.openfreebuds.se.desktop

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Platform
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Raw Linux Bluetooth RFCOMM socket (channel 16), matching how OpenFreebuds
 * talks to Huawei earbuds on desktop: open `AF_BLUETOOTH` socket and
 * `connect()` straight to the device address on the fixed SPP channel.
 */
class RfcommSocket(address: String) : Closeable {

    private val fd: Int

    init {
        fd = libc.socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM)
        if (fd < 0) {
            throw IOException("socket(AF_BLUETOOTH) failed, errno ${Native.getLastError()}")
        }

        val sa = ByteBuffer.allocate(SOCKADDR_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        sa.putShort(0, AF_BLUETOOTH.toShort())
        val mac = address.split(":").map { it.toInt(16).toByte() }
        check(mac.size == 6) { "Invalid bluetooth address: $address" }
        for (i in 0 until 6) sa.put(2 + i, mac[i])
        sa.put(8, CHANNEL.toByte())

        val mem = Memory(SOCKADDR_SIZE.toLong())
        mem.write(0, sa.array(), 0, SOCKADDR_SIZE)
        val rc = libc.connect(fd, mem, SOCKADDR_SIZE)
        if (rc != 0) {
            close()
            throw IOException(
                "RFCOMM connect to $address:${CHANNEL} failed, errno ${Native.getLastError()}",
            )
        }
    }

    fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val mem = Memory(length.toLong())
        val n = libc.read(fd, mem, length)
        if (n > 0) mem.read(0, buffer, offset, n)
        return n
    }

    fun write(bytes: ByteArray): Int {
        val mem = Memory(bytes.size.toLong())
        mem.write(0, bytes, 0, bytes.size)
        val n = libc.write(fd, mem, bytes.size)
        if (n < 0) throw IOException("RFCOMM write failed, errno ${Native.getLastError()}")
        return n
    }

    override fun close() {
        if (fd >= 0) libc.close(fd)
    }

    private interface NativeLibc : Library {
        fun socket(domain: Int, type: Int, protocol: Int): Int
        fun connect(fd: Int, address: Pointer, addressLength: Int): Int
        fun read(fd: Int, buffer: Pointer, count: Int): Int
        fun write(fd: Int, buffer: Pointer, count: Int): Int
        fun close(fd: Int): Int
    }

    companion object {
        private const val AF_BLUETOOTH = 31
        private const val BTPROTO_RFCOMM = 3
        private const val SOCK_STREAM = 1
        private const val CHANNEL = 16
        private const val SOCKADDR_SIZE = 10

        private val libc: NativeLibc by lazy {
            Native.load(Platform.C_LIBRARY_NAME, NativeLibc::class.java)
        }
    }
}
