package com.example.offgridchat.ble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID

class BleClient(private val ctx: Context) {

    private var gatt: BluetoothGatt? = null
    private var connected = false
    private var connectionCallback: ((String) -> Unit)? = null

    private val incoming_ = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incoming: SharedFlow<ByteArray> = incoming_

    private fun hasPerm(name: String) =
        ContextCompat.checkSelfPermission(ctx, name) == PackageManager.PERMISSION_GRANTED

    fun canUseBle(): Boolean {
        val needScan = Build.VERSION.SDK_INT >= 31 && !hasPerm(Manifest.permission.BLUETOOTH_SCAN)
        val needConn = Build.VERSION.SDK_INT >= 31 && !hasPerm(Manifest.permission.BLUETOOTH_CONNECT)
        val needLoc = Build.VERSION.SDK_INT < 31 && !hasPerm(Manifest.permission.ACCESS_FINE_LOCATION)
        return !(needScan || needConn || needLoc)
    }

    @RequiresPermission(anyOf = [
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    ])
    fun connectAuto(mac: String, onStatus: (String) -> Unit = {}) {
        println("[DEBUG] BLE: Attempting to connect to MAC: $mac")
        val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val dev = adapter.getRemoteDevice(mac)
        println("[DEBUG] BLE: Device created, starting GATT connection...")
        onStatus("connecting")
        gatt = dev.connectGatt(ctx, false, gattCb)
    }

    @RequiresPermission(anyOf = [
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    ])
    fun scanAndConnect(onStatus: (String) -> Unit = {}) {
        connectionCallback = onStatus
        println("[DEBUG] BLE: Starting scan for ESP32 devices...")
        val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        
        if (!adapter.isEnabled) {
            println("[DEBUG] BLE: Bluetooth adapter is disabled!")
            onStatus("bluetooth_disabled")
            return
        }
        
        if (!adapter.isMultipleAdvertisementSupported) {
            println("[DEBUG] BLE: Multiple advertisement not supported")
        }
        
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            println("[DEBUG] BLE: BLE scanner is null!")
            onStatus("scanner_null")
            return
        }
        
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val deviceName = device.name
                println("[DEBUG] BLE: Found device: $deviceName (${device.address})")
                
                // Look for ESP32 devices - check for exact name match first
                if (deviceName?.contains("OffGridChat-ESP32", ignoreCase = true) == true ||
                    deviceName?.contains("ESP32", ignoreCase = true) == true || 
                    deviceName?.contains("OffGridChat", ignoreCase = true) == true) {
                    println("[DEBUG] BLE: Found ESP32 device: $deviceName, connecting...")
                    scanner.stopScan(this)
                    onStatus("connecting")
                    gatt = device.connectGatt(ctx, false, gattCb)
                } else {
                    println("[DEBUG] BLE: Ignoring device: $deviceName (not ESP32)")
                }
            }
            
            override fun onScanFailed(errorCode: Int) {
                println("[DEBUG] BLE: Scan failed with error: $errorCode")
                onStatus("scan_failed")
            }
        }
        
        // Start scanning for 10 seconds
        scanner.startScan(scanCallback)
        
            // Stop scanning after 10 seconds if no device found
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                scanner.stopScan(scanCallback)
                if (!connected) {
                    println("[DEBUG] BLE: Scan timeout - no ESP32 found")
                    println("[DEBUG] BLE: Trying direct MAC connection as fallback...")
                    // Try direct connection to known MAC addresses
                    tryDirectMacConnection(onStatus)
                }
            }, 10000)
    }

    private fun tryDirectMacConnection(onStatus: (String) -> Unit) {
        val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val bondedDevices = adapter.bondedDevices
        
        println("[DEBUG] BLE: Checking bonded devices...")
        for (device in bondedDevices) {
            println("[DEBUG] BLE: Bonded device: ${device.name} (${device.address})")
            if (device.name?.contains("ESP32", ignoreCase = true) == true || 
                device.name?.contains("OffGridChat", ignoreCase = true) == true) {
                println("[DEBUG] BLE: Found bonded ESP32: ${device.name}, connecting...")
                onStatus("connecting")
                gatt = device.connectGatt(ctx, false, gattCb)
                return
            }
        }
        
        // If no bonded devices, try to get device by MAC
        try {
            val macAddress = BleUuids.BOARD_A_MAC
            val device = adapter.getRemoteDevice(macAddress)
            println("[DEBUG] BLE: Trying direct connection to MAC: $macAddress")
            onStatus("connecting")
            gatt = device.connectGatt(ctx, false, gattCb)
        } catch (e: Exception) {
            println("[DEBUG] BLE: Direct MAC connection failed: ${e.message}")
            onStatus("timeout")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        gatt?.close()
        gatt = null
        connected = false
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun write(data: ByteArray): Boolean {
        val g = gatt ?: return false
        val svc = g.getService(BleUuids.SERVICE) ?: return false
        val ch = svc.getCharacteristic(BleUuids.CHAR_WRITE) ?: return false
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ch.value = data
        return g.writeCharacteristic(ch)
    }

    private val gattCb = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            println("[DEBUG] BLE: Connection state changed - status: $status, newState: $newState")
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                connected = true
                println("[DEBUG] BLE: Connected! Discovering services...")
                connectionCallback?.invoke("connected")
                gatt.discoverServices()
            } else {
                connected = false
                println("[DEBUG] BLE: Disconnected - status: $status, newState: $newState")
                connectionCallback?.invoke("disconnected")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            println("[DEBUG] BLE: Services discovered - status: $status")
            val svc = gatt.getService(BleUuids.SERVICE)
            if (svc == null) {
                println("[DEBUG] BLE: ERROR - Nordic UART service not found!")
                return
            }
            println("[DEBUG] BLE: Nordic UART service found")
            
            val notifyCh = svc.getCharacteristic(BleUuids.CHAR_NOTIFY)
            if (notifyCh == null) {
                println("[DEBUG] BLE: ERROR - Notify characteristic not found!")
                return
            }
            println("[DEBUG] BLE: Notify characteristic found, enabling notifications...")
            gatt.setCharacteristicNotification(notifyCh, true)

            // Enable CCCD notifications
            val cccd = notifyCh.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(cccd) // still needed on many devices
                println("[DEBUG] BLE: Notifications enabled - connection complete!")
                connectionCallback?.invoke("connected")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == BleUuids.CHAR_NOTIFY) {
                val bytes = characteristic.value ?: return
                incoming_.tryEmit(bytes)
            }
        }
    }
}
