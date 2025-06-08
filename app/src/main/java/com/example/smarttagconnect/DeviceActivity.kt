package com.example.smarttagconnect

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager.OnActivityResultListener
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.UUID

class DeviceActivity : AppCompatActivity() {
    private lateinit var consoleOutput: TextView
    private lateinit var commandInput: EditText
    private lateinit var sendButton: Button

    private val consoleBuffer = StringBuilder()

    private var bleService: BLEConnectionService? = null
    private var connected = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as BLEConnectionService.LocalBinder
            bleService = binder.getService()
            val address = intent.getStringExtra("deviceAddress")
            connected = bleService?.connect(address!!) == true

            bleService?.onDataReceived = { message ->
                runOnUiThread {
                    appendToConsole("> $message\n")
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bleService = null
            connected = false
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, BLEConnectionService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        consoleOutput = findViewById(R.id.consoleOutput)
        commandInput = findViewById(R.id.commandInput)
        sendButton = findViewById(R.id.sendButton)

        sendButton.setOnClickListener{
            val command = commandInput.text.toString()
            if (command.isNotBlank()) {
                appendToConsole("> $command")
                commandInput.text.clear()
                sendCommandToDevice(command)
            }
        }
    }

    fun appendToConsole(text: String) {
        consoleBuffer.append(text).append("\n")
        consoleOutput.text = consoleBuffer.toString()

        // Auto-scroll
        val scrollView = consoleOutput.parent as ScrollView
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun sendCommandToDevice(command: String) {
        val characteristicUUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val serviceUUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        bleService?.sendData(command)
    }
}