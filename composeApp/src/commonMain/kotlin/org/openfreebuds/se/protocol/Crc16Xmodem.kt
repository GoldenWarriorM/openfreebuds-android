package org.openfreebuds.se.protocol

/**
 * CRC-16/XMODEM checksum as used by the Huawei MBB protocol.
 *
 * Polynomial 0x1021, initial value 0x0000, no input/output reflection,
 * no final xor. Matches OpenFreebuds `crc16_xmodem` and the Dart
 * `Crc16Xmodem` implementation from FreeBuddy.
 */
object Crc16Xmodem {

    private val table = IntArray(256)

    init {
        for (i in 0 until 256) {
            var crc = i shl 8
            for (j in 0 until 8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
            table[i] = crc
        }
    }

    /** Returns the 16-bit checksum of [data]. */
    fun calculate(data: ByteArray): Int {
        var crc = 0
        for (b in data) {
            crc = table[((crc shr 8) xor (b.toInt() and 0xFF)) and 0xFF] xor (crc shl 8)
            crc = crc and 0xFFFF
        }
        return crc
    }
}
