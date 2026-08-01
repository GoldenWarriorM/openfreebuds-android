package org.openfreebuds.se.protocol

import org.openfreebuds.se.model.BatteryLevels
import org.openfreebuds.se.model.DeviceInfo
import org.openfreebuds.se.model.TapAction

/**
 * FreeBuds SE command set.
 *
 * See OpenFreebuds `openfreebuds/driver/huawei/handler/` and FreeBuddy
 * `lib/headphones/huawei/freebuds4i/freebuds4i_impl.dart` for the protocol
 * reference.
 */
object SeCommands {

    const val BATTERY_READ_SERVICE = 1
    const val BATTERY_READ_COMMAND = 8

    const val BATTERY_NOTIFY_SERVICE = 1
    const val BATTERY_NOTIFY_COMMAND = 0x27

    const val INFO_SERVICE = 1
    const val INFO_COMMAND = 7

    const val DUAL_TAP_READ_SERVICE = 1
    const val DUAL_TAP_READ_COMMAND = 0x20

    const val DUAL_TAP_WRITE_SERVICE = 1
    const val DUAL_TAP_WRITE_COMMAND = 0x1F

    const val LOG_SERVICE = 10
    const val LOG_COMMAND = 13

    fun batteryRead(): MbbPackage =
        MbbPackage(BATTERY_READ_SERVICE, BATTERY_READ_COMMAND).apply {
            params[1] = byteArrayOf()
            params[2] = byteArrayOf()
            params[3] = byteArrayOf()
        }

    fun infoRead(): MbbPackage =
        MbbPackage(INFO_SERVICE, INFO_COMMAND).apply {
            for (i in 0 until 32) params[i] = byteArrayOf()
        }

    fun doubleTapRead(): MbbPackage =
        MbbPackage(DUAL_TAP_READ_SERVICE, DUAL_TAP_READ_COMMAND).apply {
            params[1] = byteArrayOf()
            params[2] = byteArrayOf()
        }

    fun doubleTapWrite(left: TapAction, right: TapAction): MbbPackage =
        MbbPackage(DUAL_TAP_WRITE_SERVICE, DUAL_TAP_WRITE_COMMAND).apply {
            params[1] = byteArrayOf(left.code.toByte())
            params[2] = byteArrayOf(right.code.toByte())
        }

    /** Parses a battery package (either 1:8 or 1:39 response). */
    fun parseBattery(pkg: MbbPackage): BatteryLevels {
        var left: Int? = null
        var right: Int? = null
        var case: Int? = null

        val tws = pkg.param(2)
        if (tws != null && tws.size == 3) {
            left = tws[0].toInt() and 0xFF
            right = tws[1].toInt() and 0xFF
            case = tws[2].toInt() and 0xFF
        } else if (tws != null && tws.size == 1) {
            left = tws[0].toInt() and 0xFF
        }

        if (left == 0) left = null
        if (right == 0) right = null
        if (case == 0) case = null

        var chargingLeft = false
        var chargingRight = false
        var chargingCase = false
        val status = pkg.param(3)
        if (status != null) {
            when {
                status.size >= 3 -> {
                    chargingLeft = status[0].toInt() == 1
                    chargingRight = status[1].toInt() == 1
                    chargingCase = status[2].toInt() == 1
                }
                status.size == 1 -> {
                    chargingLeft = status[0].toInt() == 1
                    chargingRight = status[0].toInt() == 1
                    chargingCase = status[0].toInt() == 1
                }
            }
        }

        return BatteryLevels(
            left = left,
            right = right,
            case = case,
            chargingLeft = chargingLeft,
            chargingRight = chargingRight,
            chargingCase = chargingCase,
        )
    }

    private fun parseString(bytes: ByteArray): String? =
        bytes.toString(Charsets.UTF_8).takeIf { it.isNotBlank() }

    /** Parses a device info package (1:7 response). */
    fun parseInfo(pkg: MbbPackage): DeviceInfo {
        val info = pkg.param(15)?.let(::parseString)
            ?: pkg.param(10)?.let(::parseString)
            ?: "FreeBuds SE"
        return DeviceInfo(
            name = info,
            hardwareVersion = pkg.param(3)?.let(::parseString),
            softwareVersion = pkg.param(7)?.let(::parseString),
            serial = pkg.param(9)?.let(::parseString),
        )
    }

    private val tapOptions = mapOf(
        -1 to TapAction.OFF,
        0 to TapAction.VOICE_ASSISTANT,
        1 to TapAction.PLAY_PAUSE,
        2 to TapAction.NEXT,
        7 to TapAction.PREVIOUS,
    )

    /** Parses a double tap config package (1:0x20 response). */
    fun parseDoubleTap(pkg: MbbPackage): Pair<TapAction, TapAction>? {
        val left = pkg.param(1)
        val right = pkg.param(2)
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) return null
        val leftAction = tapOptions[left[0].toInt()]
        val rightAction = tapOptions[right[0].toInt()]
        if (leftAction == null || rightAction == null) return null
        return leftAction to rightAction
    }
}
