package com.example.offgridchat.ble

import java.util.UUID

object BleUuids {
    // Nordic UART (NUS)
    val SERVICE: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val CHAR_WRITE: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")   // phone -> ESP (WRITE/WRITE_NR)
    val CHAR_NOTIFY: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")  // ESP -> phone (NOTIFY)

    // Your board MACs
    const val BOARD_A_MAC = "48:CA:43:3A:67:51"
    const val BOARD_B_MAC = "64:E8:33:5C:2C:41"
}
