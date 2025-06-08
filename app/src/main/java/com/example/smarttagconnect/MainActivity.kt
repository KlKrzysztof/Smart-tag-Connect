package com.example.smarttagconnect

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    val activity: Context = this
    lateinit var buttonScan: Button
    lateinit var BLEDeviceView: ListView
    lateinit var adapter: ArrayAdapter<String>
    val itemList = mutableListOf<String>()
    val foundDevices = mutableListOf<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 1)
        }

        buttonScan = findViewById(R.id.Button_scan)

        buttonScan.setOnClickListener({button_Scan_Click()})

        BLEDeviceView = findViewById(R.id.BLE_Device_View)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemList)
        BLEDeviceView.adapter = adapter

        BLEDeviceView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = itemList[position]
            Toast.makeText(this, "Connecting to: $selectedItem", Toast.LENGTH_SHORT).show()

            val selectedDevice = foundDevices[position]

            selectedDevice.connectGatt(this, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {

                    BLEConnectionStatus(newState)

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt.discoverServices()
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    val services = gatt.services
                    for (service in services) {
                        Log.d("BLE", "service: ${service.uuid}")
                    }
                }
            })
        }
    }

    fun BLEConnectionStatus (state: Int){
        if (state == BluetoothProfile.STATE_CONNECTED) {
            Log.d("BLE", "Connection successful")
            Toast.makeText(this, "Connection successful", Toast.LENGTH_SHORT).show()

        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d("BLE", "disconnected")
            Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun button_Scan_Click() {

        if (ContextCompat.checkSelfPermission(this, BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    BLUETOOTH_SCAN,
                    BLUETOOTH_CONNECT,
                    ACCESS_FINE_LOCATION
                ),
                2
            )
            return
        }

        BLEScan()
    }

    fun BLEScan(){
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Urządzenie nie obsługuje Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = result.device.name ?: result.scanRecord?.deviceName ?: "Nieznane urządzenie"

                if(!itemList.contains(name)) {
                    Log.d("BLE", "Znaleziono urządzenie: ${device.name} - ${device.address}")
                    itemList.add(name)
                    adapter.notifyDataSetChanged()
                    foundDevices.add(result.device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLE", "Błąd skanowania: $errorCode")
            }
        }

        scanner.startScan(scanCallback)
        Toast.makeText(this, "Skanowanie BLE rozpoczęte...", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when{
            resultCode == 1 ->{
                BLEScan()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when {
            requestCode == 1 ->{

            }

            requestCode == 2 ->{
                BLEScan()
            }
        }
    }
}