package org.openfreebuds.se.protocol

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MbbPackageTest {

    @Test
    fun roundTrip() {
        val pkg = MbbPackage(1, 8).apply {
            params[1] = byteArrayOf()
            params[2] = byteArrayOf()
            params[3] = byteArrayOf()
        }
        val bytes = pkg.toBytes()
        val parsed = MbbPackage.fromBytes(bytes)
        assertEquals(1, parsed.serviceId)
        assertEquals(8, parsed.commandId)
        assertEquals(pkg.params.keys, parsed.params.keys)
        pkg.params.forEach { (key, value) ->
            assertContentEquals(value, parsed.params[key], "param $key")
        }
    }

    @Test
    fun lengthByteIsParamsPlus3() {
        val pkg = MbbPackage(1, 8).apply {
            params[1] = byteArrayOf()
            params[2] = byteArrayOf()
            params[3] = byteArrayOf()
        }
        val bytes = pkg.toBytes()
        val paramsSize = pkg.params.values.sumOf { it.size + 2 }
        assertEquals(paramsSize + 3, bytes[2].toInt() and 0xFF)
    }

    @Test
    fun parsesReferencePacket() {
        // Known-good battery notify packet from FreeBuddy PROTOCOL.md.
        val bytes = hexToBytes(
            "5a001000012701015a02035a640f0303000001bed9",
        )
        val pkg = MbbPackage.fromBytes(bytes)
        assertEquals(1, pkg.serviceId)
        assertEquals(0x27, pkg.commandId)
        assertContentEquals(byteArrayOf(0x5A.toByte()), pkg.param(1))
        assertContentEquals(byteArrayOf(0x5A, 0x64, 0x0F), pkg.param(2))
        assertContentEquals(byteArrayOf(0x00, 0x00, 0x01), pkg.param(3))
    }

    @Test
    fun streamParserSplitsMultiplePackets() {
        val a = MbbPackage(1, 8).apply {
            params[1] = byteArrayOf()
            params[2] = byteArrayOf()
            params[3] = byteArrayOf()
        }.toBytes()
        val b = MbbPackage(1, 0x27).apply {
            params[2] = byteArrayOf(60, 70, 80)
        }.toBytes()

        val parser = MbbStreamParser()
        val whole = parser.push(a + b)
        assertEquals(2, whole.size)

        val split = MbbStreamParser()
        val first = split.push(a.copyOfRange(0, 4))
        val second = split.push(a.copyOfRange(4, a.size) + b.copyOfRange(0, 3))
        val third = split.push(b.copyOfRange(3, b.size))
        assertEquals(0, first.size)
        assertEquals(1, second.size)
        assertEquals(8, second[0].commandId)
        assertEquals(1, third.size)
        assertEquals(0x27, third[0].commandId)
        assertEquals(0x27, whole[1].commandId)
    }

    @Test
    fun parseBatteryLevels() {
        val pkg = MbbPackage(1, 0x27).apply {
            params[2] = byteArrayOf(90, 85, 40)
            params[3] = byteArrayOf(0, 1, 0)
        }
        val battery = SeCommands.parseBattery(pkg)
        assertEquals(90, battery.left)
        assertEquals(85, battery.right)
        assertEquals(40, battery.case)
        assertEquals(false, battery.chargingLeft)
        assertEquals(true, battery.chargingRight)
        assertEquals(false, battery.chargingCase)
        assertNotNull(battery.left)
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0)
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
