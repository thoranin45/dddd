package com.example.dddd

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class wifi : AppCompatActivity() {
    private lateinit var uploadTextView: TextView
    private lateinit var downloadTextView: TextView
    private lateinit var rttTextView: TextView
    private lateinit var ssidTextView: TextView
    private lateinit var pingTextView: TextView
    private lateinit var startButton: Button
    private lateinit var channelTextView: TextView

    // CoroutineScope for all coroutines in MainActivity
    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi)

        uploadTextView = findViewById(R.id.uploadTextView)
        downloadTextView = findViewById(R.id.downloadTextView)
        rttTextView = findViewById(R.id.rttTextView)
        ssidTextView = findViewById(R.id.ssidTextView)
        pingTextView = findViewById(R.id.pingTextView)
        startButton = findViewById(R.id.startButton)
        channelTextView = findViewById(R.id.channelTextView)
    }

    // Function to start fetching WiFi information when the button is clicked
    @RequiresApi(Build.VERSION_CODES.Q)
    fun startFetching(view: View) {
        startButton.isEnabled = false // Disable button during fetching

        // Start Coroutine to fetch WiFi info
        mainScope.launch {
            val wifiInfo = fetchWifiInfo()
            val ssid = fetchSSID()
            val ping = fetchPing()
            val frequency = wifiInfo?.frequency ?: 0
            val channel = getWifiChannel(frequency)

            updateWifiInfo(wifiInfo)
            ssidTextView.text = "SSID: $ssid"
            pingTextView.text = "Ping: $ping ms"
            channelTextView.text = "Channel: $channel"

            startButton.isEnabled = true // Enable the button after fetching
        }
    }

    // Coroutine to fetch WiFi information
    private suspend fun fetchWifiInfo(): WifiInfo? = withContext(Dispatchers.IO) {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.connectionInfo
    }

    // Function to fetch SSID of the connected WiFi network
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun fetchSSID(): String = withContext(Dispatchers.IO) {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        wifiInfo.ssid
    }

    // Function to fetch Ping
    private suspend fun fetchPing(): Long = withContext(Dispatchers.IO) {
        val command = "/system/bin/ping -c 1 google.com"
        val runtime = Runtime.getRuntime()
        val process = runtime.exec(command)
        val inputStream = BufferedReader(InputStreamReader(process.inputStream))
        val line = inputStream.readText()
        val regex = "(?<=time=)[0-9]+".toRegex()
        val result = regex.find(line)
        result?.value?.toLong() ?: -1
    }

    // Function to update UI with WiFi information
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateWifiInfo(wifiInfo: WifiInfo?) {
        wifiInfo?.let {
            val uploadSpeed = "Upload: ${wifiInfo.txLinkSpeedMbps} Mbps"
            val downloadSpeed = "Download: ${wifiInfo.rxLinkSpeedMbps} Mbps"
            val rssi = "RSSI: ${wifiInfo.rssi} dBm"

            // Update TextViews with WiFi information
            uploadTextView.text = uploadSpeed
            downloadTextView.text = downloadSpeed
            rttTextView.text = rssi
        }
    }

    private fun getWifiChannel(frequency: Int): Int {
        return when (frequency) {
            in 2412..2484 -> (frequency - 2412) / 5 + 1
            in 5170..5825 -> (frequency - 5170) / 5 + 34
            else -> -1 // Unknown channel
        }
    }

    // Override onDestroy to cancel all coroutines when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}
