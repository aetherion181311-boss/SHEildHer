package com.sosring.ble

import kotlinx.coroutines.flow.Flow

// ─── BLE UUIDs for DA14531 ────────────────────────────────────────────────────
object BleConstants {
    const val SOS_SERVICE_UUID        = "12345678-1234-1234-1234-1234567890AB"
    const val TOUCH_PATTERN_CHAR_UUID = "12345678-1234-1234-1234-1234567890AC"
    const val ACK_CHAR_UUID           = "12345678-1234-1234-1234-1234567890AD"
    const val RING_DEVICE_NAME        = "SOS-Ring"

    // Pattern bytes sent by DA14531
    const val TRIGGER_PATTERN: Byte = 0x01
    const val CANCEL_PATTERN:  Byte = 0x02

    // ACK bytes sent back to ring
    const val ACK_TRIGGER: Byte = 0xA1.toByte()
    const val ACK_CANCEL:  Byte = 0xA2.toByte()
}

// ─── Domain models ────────────────────────────────────────────────────────────
sealed class TouchPattern {
    object Trigger : TouchPattern()
    object Cancel  : TouchPattern()
    data class Unknown(val rawBytes: ByteArray) : TouchPattern()
}

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Scanning     : BleConnectionState()
    object Connecting   : BleConnectionState()
    object Connected    : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

// ─── Platform contract ────────────────────────────────────────────────────────
/**
 * Platform-specific BLE manager. Implemented in androidMain / iosMain.
 * Common business logic talks only to this interface.
 */
interface BleManager {
    /** Emits connection state changes. */
    val connectionState: Flow<BleConnectionState>

    /** Emits decoded touch patterns from the ring. */
    val touchPatterns: Flow<TouchPattern>

    /** Start scanning and connect to the SOS ring. */
    suspend fun startScanAndConnect()

    /** Send an acknowledgement byte back to the ring. */
    suspend fun sendAcknowledgement(ack: Byte): Boolean

    /** Release resources and disconnect. */
    fun disconnect()
}
