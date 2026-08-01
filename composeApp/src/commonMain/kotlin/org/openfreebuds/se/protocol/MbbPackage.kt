package org.openfreebuds.se.protocol

import kotlin.math.min

/**
 * A single MBB (Huawei SPP) protocol message.
 *
 * Byte layout:
 * ```
 * [0x5A, 0x00, len(2 bytes big-endian), 0x00, serviceId, commandId, TLV..., crc(2 bytes)]
 * ```
 * The `len` field is the size of everything after byte[3] plus one, i.e.
 * `commandId + parameters + crc + 1`. TLV entries are
 * `[paramType, paramLength, paramValue...]`.
 */
class MbbPackage(
    val serviceId: Int,
    val commandId: Int,
    val params: LinkedHashMap<Int, ByteArray> = LinkedHashMap(),
) {

    fun param(type: Int): ByteArray? = params[type]

    fun param(type: Int, default: ByteArray): ByteArray = params[type] ?: default

    fun toBytes(): ByteArray {
        val body = ArrayList<Byte>(8)
        body.add(serviceId.toByte())
        body.add(commandId.toByte())
        for ((type, value) in params) {
            body.add(type.toByte())
            body.add(value.size.toByte())
            value.forEach { body.add(it) }
        }

        val length = body.size + 1

        val payload = ByteArray(body.size + 6)
        payload[0] = 0x5A
        payload[1] = 0x00
        payload[2] = length.toByte()
        payload[3] = 0x00
        System.arraycopy(body.toByteArray(), 0, payload, 4, body.size)

        val crc = Crc16Xmodem.calculate(payload.copyOfRange(0, payload.size - 2))
        payload[payload.size - 2] = (crc shr 8).toByte()
        payload[payload.size - 1] = (crc and 0xFF).toByte()
        return payload
    }

    override fun toString(): String {
        val paramsStr = params.entries.joinToString(", ") { (k, v) ->
            "$k=${v.joinToString("") { "%02x".format(it.toInt() and 0xFF) }}"
        }
        return "MbbPackage(service=$serviceId, cmd=$commandId, params={$paramsStr})"
    }

    companion object {

        /** Total wire length of a package given its length byte. */
        fun totalLength(lengthByte: Int): Int = lengthByte + 5

        /**
         * Parses a complete package. Caller must ensure [data] is exactly one
         * package (already split by [MbbStreamParser]).
         */
        fun fromBytes(data: ByteArray, verifyChecksum: Boolean = true): MbbPackage {
            require(data.size >= 8) { "Package too short: ${data.size} bytes" }
            require(data[0] == 0x5A.toByte() && data[1] == 0x00.toByte() && data[3] == 0x00.toByte()) {
                "Invalid magic bytes"
            }
            if (verifyChecksum) {
                val crc = Crc16Xmodem.calculate(data.copyOfRange(0, data.size - 2))
                val crcLo = data[data.size - 2].toInt() and 0xFF
                val crcHi = data[data.size - 1].toInt() and 0xFF
                require((crc shr 8) and 0xFF == crcLo && crc and 0xFF == crcHi) {
                    "Checksum mismatch"
                }
            }

            val serviceId = data[4].toInt() and 0xFF
            val commandId = data[5].toInt() and 0xFF
            val params = LinkedHashMap<Int, ByteArray>()

            var position = 6
            val end = min(data.size - 2, data.size)
            while (position + 1 < end) {
                val type = data[position].toInt() and 0xFF
                val length = data[position + 1].toInt() and 0xFF
                val valueStart = position + 2
                val valueEnd = min(valueStart + length, end)
                if (valueEnd < valueStart) break
                params[type] = data.copyOfRange(valueStart, valueEnd)
                position = valueEnd
            }

            return MbbPackage(serviceId, commandId, params)
        }
    }
}

/**
 * Incrementally splits a raw byte stream into complete [MbbPackage]s.
 * Packages may be concatenated and/or split across read chunks.
 */
class MbbStreamParser {

    private val buffer = ArrayList<Byte>()

    /** Pushes received bytes and returns any complete packages found. */
    fun push(chunk: ByteArray): List<MbbPackage> {
        chunk.forEach { buffer.add(it) }
        val result = ArrayList<MbbPackage>()
        while (true) {
            val pkg = tryExtractNext() ?: break
            result.add(pkg)
        }
        return result
    }

    private fun tryExtractNext(): MbbPackage? {
        resync()
        if (buffer.size < 8) return null

        val lengthByte = buffer[2].toInt() and 0xFF
        val total = MbbPackage.totalLength(lengthByte)
        if (buffer.size < total) return null

        val pkgBytes = ByteArray(total)
        for (i in 0 until total) pkgBytes[i] = buffer[i]
        repeat(total) { buffer.removeAt(0) }

        return try {
            MbbPackage.fromBytes(pkgBytes)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun resync() {
        while (buffer.isNotEmpty()) {
            if (buffer[0] == 0x5A.toByte() &&
                (buffer.size < 2 || buffer[1] == 0x00.toByte())
            ) return
            buffer.removeAt(0)
        }
    }
}
