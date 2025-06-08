package com.example.smarttagconnect

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.UUID
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager

class BLEConnectionService : Service() {

    private var bluetoothGatt: BluetoothGatt? = null
    private val binder = LocalBinder()


    private val NUS_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // send
    private val TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // receive

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    var onDataReceived: ((String) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): BLEConnectionService = this@BLEConnectionService
    }

    override fun onBind(intent: Intent): IBinder {
        return this.binder
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String): Boolean {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        return true
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    @SuppressLint("MissingPermission")
    fun sendData(data: String) {
        rxCharacteristic?.let {
            it.value = data.toByteArray(Charsets.UTF_8)
            bluetoothGatt?.writeCharacteristic(it)
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BLE", "Połączono z GATT server")
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("BLE", "Rozłączono z GATT server")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(NUS_SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID)
                    txCharacteristic = service.getCharacteristic(TX_CHAR_UUID)

                    // notification alert
                    txCharacteristic?.let {
                        gatt.setCharacteristicNotification(it, true)
                        val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor?.let { d ->
                            d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(d)
                        }
                    }
                }
            } else {
                Log.w("BLE", "Błąd przy odkrywaniu usług: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == TX_CHAR_UUID) {
                val data = characteristic.value.toString(Charsets.UTF_8)
                onDataReceived?.invoke(data)
            }
        }
    }
}
