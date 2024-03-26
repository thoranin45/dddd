package com.example.dddd

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.CellIdentityLte
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch




class MainActivity2 : AppCompatActivity() {
    private companion object {
        const val PERMISSION_REQUEST_CODE = 1
    }

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private lateinit var plmnTextView: TextView
    private lateinit var bandInfoTextView: TextView
    private lateinit var bandwidthTextView: TextView
    private lateinit var pciTextView: TextView
    private lateinit var tacTextView: TextView
    private lateinit var rssiTextView: TextView
    private lateinit var rsrpTextView: TextView
    private lateinit var rsrqTextView: TextView
    private lateinit var sinrTextView: TextView
    private lateinit var startButton: Button
    private lateinit var cellIdTextView: TextView
    private lateinit var earfcnDLTextView: TextView

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // Initialize views
        plmnTextView = findViewById(R.id.plmnTextView)
        bandInfoTextView = findViewById(R.id.bandInfoTextView)
        bandwidthTextView = findViewById(R.id.bandwidthTextView)
        pciTextView = findViewById(R.id.pciTextView)
        tacTextView = findViewById(R.id.tacTextView)
        rssiTextView = findViewById(R.id.rssiTextView)
        rsrpTextView = findViewById(R.id.rsrpTextView)
        rsrqTextView = findViewById(R.id.rsrqTextView)
        sinrTextView = findViewById(R.id.sinrTextView)
        startButton = findViewById(R.id.startButton)
        cellIdTextView = findViewById(R.id.cellIdTextView)
        earfcnDLTextView = findViewById(R.id.earfcnDLTextView)

        startButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (checkAndRequestPermission()) {
                    startFetching()
                }
            } else {
                startFetching()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startFetching() {
        startButton.isEnabled = false // Disable button during fetching

        mainScope.launch {
            getAllcelllinfo()

            startButton.isEnabled = true // Enable the button after fetching
        }
    }

    private fun checkAndRequestPermission(): Boolean {
        val permissionsToRequest = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsGranted = permissionsToRequest.map {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }.all { it }

        if (!permissionsGranted) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest,
                PERMISSION_REQUEST_CODE
            )
        }

        return permissionsGranted
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startFetching()
            } else {
                Log.e("SignalInfoActivity", "Permission denied")
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getAllcelllinfo() {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val cellInfoList = tm.allCellInfo
        for (cellInfo in cellInfoList) {
            if (cellInfo is CellInfoLte) {
                val rssi = cellInfo.cellSignalStrength.rssi
                val rsrp = cellInfo.cellSignalStrength.rsrp
                val rsrq = cellInfo.cellSignalStrength.rsrq
                val sinr = calculateSinr(rsrp, rssi, rsrq) // คำนวณ SINR ด้วยฟังก์ชัน calculateSinr

                rssiTextView.text = "RSSI: $rssi dBm"
                rsrpTextView.text = "RSRP: $rsrp dBm"
                rsrqTextView.text = "RSRQ: $rsrq dB"
                sinrTextView.text = "SINR: $sinr dB"

                // Set color based on RSRP
                val rsrpColor = when {
                    rsrp >= -94 -> ContextCompat.getColor(this, R.color.Color2) // Excellent
                    rsrp in -98 until -94 -> ContextCompat.getColor(this, R.color.Color6) // Good
                    rsrp in -102 until -98 -> ContextCompat.getColor(this, R.color.Color3) // Fair
                    rsrp in -105 until -102 -> ContextCompat.getColor(this, R.color.Color7) // Excellent
                    rsrp in -109 until -105 -> ContextCompat.getColor(this, R.color.Color8) // Good
                    rsrp in -113 until -109 -> ContextCompat.getColor(this, R.color.Color4) // Fair
                    rsrp < -113 -> ContextCompat.getColor(this, R.color.Color5)
                    else -> ContextCompat.getColor(this, R.color.defaultColor) // Poor
                }

                // Set background color
                val rsrpColorView: View = findViewById(R.id.rsrpColorView)
                rsrpColorView.setBackgroundColor(rsrpColor)
            }
        }
        for (cellInfo in cellInfoList) {
            if (cellInfo is CellInfoLte) {
                val lteSignalStrength: CellIdentityLte = cellInfo.cellIdentity

                // ดึงค่า PLMN, Band Info, Bandwidth, PCI, TAC
                val plmn = lteSignalStrength.mcc.toString() + lteSignalStrength.mnc.toString()
                val bandInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    lteSignalStrength.bands
                } else {
                    "Not available" // ไม่สามารถดึงข้อมูลได้เนื่องจากไม่รองรับ SDK ที่เพียงพอ
                }
                val bandwidth = lteSignalStrength.bandwidth
                val pci = lteSignalStrength.pci
                val tac = lteSignalStrength.tac
                val cellId = lteSignalStrength.ci
                val earfcnDL = lteSignalStrength.earfcn


                // แสดงค่าใน TextView ของแต่ละข้อมูล
                plmnTextView.text = "PLMN: $plmn"
                bandInfoTextView.text = "Band Info: $bandInfo"
                bandwidthTextView.text = "Bandwidth: $bandwidth"
                pciTextView.text = "PCI: $pci"
                tacTextView.text = "TAC: $tac"
                cellIdTextView.text = "Cell ID: $cellId"
                // radioTechTextView.text = "Radio Access Technology: $radioAccessTech"
                earfcnDLTextView.text = "EARFCN DL: $earfcnDL"
            }
        }
    }
    fun calculateSinr(rsrp: Int, rssi: Int, rsrq: Int): Double {
        // คำนวณ interference + noise จาก rssi - rsrp
        val interferenceNoise = rssi - rsrp
        // ตรวจสอบว่า interferenceNoise เป็นศูนย์หรือไม่ หากเป็นศูนย์ให้กำหนดค่า SINR เป็นค่าไม่จำกัด
        val sinr = if (interferenceNoise != 0) {
            // คำนวณ sinr จาก rsrp / (interference + noise) * (1 / rsrq)
            rsrp.toDouble() / interferenceNoise.toDouble() * (1.0 / rsrq.toDouble())
        } else {
            Double.POSITIVE_INFINITY // กำหนดให้ SINR เป็นค่าไม่จำกัดหาก interferenceNoise เป็นศูนย์
        }
        // คืนค่า sinr
        return sinr
    }
}




