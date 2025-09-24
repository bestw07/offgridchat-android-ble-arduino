package com.example.offgridchat.ble

import android.Manifest
import android.bluetooth.*
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
        val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val dev = adapter.getRemoteDevice(mac)
        onStatus("connecting")
        gatt = dev.connectGatt(ctx, false, gattCb)
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
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true
                gatt.discoverServices()
            } else {
                connected = false
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val svc = gatt.getService(BleUuids.SERVICE) ?: return
            val notifyCh = svc.getCharacteristic(BleUuids.CHAR_NOTIFY) ?: return
            gatt.setCharacteristicNotification(notifyCh, true)

            // Enable CCCD notifications
            val cccd = notifyCh.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(cccd) // still needed on many devices
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
