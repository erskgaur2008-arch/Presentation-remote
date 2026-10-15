package com.erskgaur.presentationremote

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var deviceSpinner: Spinner
    private var devices: List<BluetoothDevice> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        requestBluetoothPermissions()
        refreshPairedDevices()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFF101827.toInt())
        }
        val title = TextView(this).apply {
            text = "ER SK GAUR\nPresentation Remote"
            textSize = 24f; gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
        }
        status = TextView(this).apply {
            text = "Not connected"; textSize = 14f
            setTextColor(0xFFB8C7DC.toInt()); gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
        }
        deviceSpinner = Spinner(this)
        val refresh = Button(this).apply { text = "Refresh paired devices"; setOnClickListener { refreshPairedDevices() } }
        val connect = Button(this).apply { text = "Connect / Reconnect"; setOnClickListener { connectSelected() } }
        preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(0xFF263246.toInt())
        }
        root.addView(title)
        root.addView(status)
        root.addView(deviceSpinner)
        root.addView(refresh)
        root.addView(connect)
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(row("◀ Previous" to "PREVIOUS", "Next ▶" to "NEXT"))
        root.addView(row("Start show" to "START", "End show" to "END"))
        root.addView(row("Black screen" to "BLACK", "Slide preview" to "PREVIEW"))
        root.addView(row("Volume −" to "VOLUME_DOWN", "Volume +" to "VOLUME_UP"))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun row(vararg actions: Pair<String, String>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            actions.forEach { (label, command) ->
                addView(Button(this@MainActivity).apply {
                    text = label
                    setOnClickListener { sendCommand(command) }
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            val needed = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 10)
        }
    }

    private fun refreshPairedDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) { status.text = "This phone has no Bluetooth"; return }
            if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                status.text = "Grant Bluetooth permission, then refresh"; return
            }
            devices = adapter.bondedDevices.toList().sortedBy { it.name ?: it.address }
            deviceSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                devices.map { "${it.name ?: "Bluetooth device"} (${it.address})" })
            status.text = if (devices.isEmpty()) "Pair your laptop in Android Bluetooth settings first" else "Select your paired laptop"
        } catch (e: Exception) { status.text = "Bluetooth error: ${e.message}" }
    }

    private fun connectSelected() {
        if (devices.isEmpty() || deviceSpinner.selectedItemPosition !in devices.indices) {
            status.text = "Pair your laptop in Android Bluetooth settings first"; return
        }
        val device = devices[deviceSpinner.selectedItemPosition]
        status.text = "Connecting to ${device.name ?: device.address}…"
        thread {
            try {
                socket?.close()
                val adapter = BluetoothAdapter.getDefaultAdapter()
                adapter?.cancelDiscovery()
                val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                newSocket.connect()
                socket = newSocket
                output = newSocket.outputStream
                runOnUiThread { status.text = "Connected to ${device.name ?: device.address}" }
                listenForReplies(newSocket)
            } catch (e: Exception) {
                runOnUiThread { status.text = "Connection failed: ${e.message}. Keep Windows receiver running, then reconnect." }
                try { socket?.close() } catch (_: Exception) {}
                socket = null; output = null
            }
        }
    }

    private fun listenForReplies(active: BluetoothSocket) {
        try {
            val reader = BufferedReader(InputStreamReader(active.inputStream))
            while (active.isConnected) {
                val line = reader.readLine() ?: break
                if (line.startsWith("PREVIEW:")) {
                    try {
                        val bytes = android.util.Base64.decode(line.substringAfter("PREVIEW:"), android.util.Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        runOnUiThread { preview.setImageBitmap(bitmap); status.text = "Connected • Preview updated" }
                    } catch (_: Exception) { runOnUiThread { status.text = "Connected • Could not decode preview" } }
                } else if (line.startsWith("STATUS:")) {
                    runOnUiThread { status.text = "Connected • " + line.substringAfter("STATUS:") }
                }
            }
        } catch (_: Exception) { }
        runOnUiThread { if (socket === active) status.text = "Disconnected — tap Connect / Reconnect" }
    }

    private fun sendCommand(command: String) {
        try {
            val stream = output ?: run { status.text = "Not connected — connect first"; return }
            stream.write((command + "\n").toByteArray(Charsets.UTF_8)); stream.flush()
        } catch (e: Exception) { status.text = "Send failed: ${e.message}" }
    }

    override fun onDestroy() {
        try { socket?.close() } catch (_: Exception) {}
        super.onDestroy()
    }
}
