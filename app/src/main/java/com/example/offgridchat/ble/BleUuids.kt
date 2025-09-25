package com.example.offgridchat.ble

import java.util.UUID

object BleUuids {
    // Your Arduino ESP32 BLE Service UUIDs (from your working code)
    val SERVICE: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val CHAR_WRITE: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")   // phone -> ESP (WRITE)
    val CHAR_NOTIFY: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")  // ESP -> phone (NOTIFY)

    // Your board MACs (not needed with auto-scan)
    const val BOARD_A_MAC = "48:CA:43:3A:67:51"
    const val BOARD_B_MAC = "64:E8:33:5C:2C:41"
}
