package org.openfreebuds.se.model

/**
 * Battery levels for left bud, right bud and charging case.
 * `null` means the value is not available (device not reporting it).
 */
data class BatteryLevels(
    val left: Int? = null,
    val right: Int? = null,
    val case: Int? = null,
    val chargingLeft: Boolean = false,
    val chargingRight: Boolean = false,
    val chargingCase: Boolean = false,
    /** True when this reading could not provide the value for the given side. */
    val missingLeft: Boolean = false,
    val missingRight: Boolean = false,
    val missingCase: Boolean = false,
) {
    val lowest: Int? get() = listOfNotNull(left, right).minOrNull()
    val isKnown: Boolean get() = left != null || right != null || case != null

    /**
     * Returns a copy where levels that this reading could not provide (null,
     * e.g. an earbud dropped out of the case / the connection) are filled from
     * the last known values of [last], together with the matching charging
     * state. So a temporarily lost earbud keeps showing its last battery level
     * and charging status.
     */
    fun withLastKnownFallback(last: BatteryLevels): BatteryLevels = BatteryLevels(
        left = left ?: last.left,
        right = right ?: last.right,
        case = case ?: last.case,
        chargingLeft = if (left != null) chargingLeft else last.chargingLeft,
        chargingRight = if (right != null) chargingRight else last.chargingRight,
        chargingCase = if (case != null) chargingCase else last.chargingCase,
        missingLeft = left == null,
        missingRight = right == null,
        missingCase = case == null,
    )

    companion object {
        val Unknown = BatteryLevels()
    }
}

/** Double tap gesture action, as reported by the FreeBuds SE. */
enum class TapAction(val code: Int) {
    OFF(-1),
    VOICE_ASSISTANT(0),
    PLAY_PAUSE(1),
    NEXT(2),
    PREVIOUS(7);

    companion object {
        fun fromCode(code: Int): TapAction =
            entries.firstOrNull { it.code == code } ?: OFF
    }
}

/** Double tap configuration for both buds. */
data class DoubleTapConfig(
    val left: TapAction = TapAction.OFF,
    val right: TapAction = TapAction.OFF,
) {
    val isDefault: Boolean get() = left == TapAction.OFF && right == TapAction.OFF
}

/** Device information reported via the 1:7 command. */
data class DeviceInfo(
    val name: String = "FreeBuds SE",
    val hardwareVersion: String? = null,
    val softwareVersion: String? = null,
    val serial: String? = null,
)

/** A bluetooth device that looks like a FreeBuds SE. */
data class SeDevice(
    val address: String,
    val name: String,
    val connected: Boolean = false,
)
