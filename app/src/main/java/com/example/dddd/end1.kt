package com.example.dddd;

import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.HttpDownloadTest
import com.example.HttpUploadTest
import com.example.PingTest

import java.text.DecimalFormat


class end1 : AppCompatActivity() {
    var getSpeedTestHostsHandler: GetSpeedTestHostsHandler? = null
    var tempBlackList: HashSet<String>? = null
    public override fun onResume() {
        super.onResume()
        getSpeedTestHostsHandler = GetSpeedTestHostsHandler()
        getSpeedTestHostsHandler!!.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end1)
        val startButton = findViewById<View>(R.id.startButton) as Button
        val dec = DecimalFormat("#.##")
        startButton.text = "Begin Test"
        tempBlackList = HashSet()
        getSpeedTestHostsHandler = GetSpeedTestHostsHandler()
        getSpeedTestHostsHandler!!.start()
        startButton.setOnClickListener {
            startButton.isEnabled = false

            //Restart test icin eger baglanti koparsa
            if (getSpeedTestHostsHandler == null) {
                getSpeedTestHostsHandler = GetSpeedTestHostsHandler()
                getSpeedTestHostsHandler!!.start()
            }
            Thread(object : Runnable {
                var rotate: RotateAnimation? = null
                var barImageView =
                    findViewById<View>(R.id.barImageView) as ImageView
                var pingTextView =
                    findViewById<View>(R.id.pingTextView) as TextView
                var downloadTextView =
                    findViewById<View>(R.id.downloadTextView) as TextView
                var uploadTextView =
                    findViewById<View>(R.id.uploadTextView) as TextView
                var  downloadResultTextView =
                    findViewById<View>(R.id.downloadResultTextView) as TextView
                var  uploadResultTextView =
                    findViewById<View>(R.id.uploadResultTextView) as TextView
                var pingResultTextView =
                    findViewById<View>(R.id.pingResultTextView) as TextView

                override fun run() {
                    runOnUiThread {
                        startButton.text = "Selecting best server based on ping..."
                    }

                    //Get egcodes.speedtest hosts
                    var timeCount = 600 //1min
                    while (!getSpeedTestHostsHandler!!.isFinished) {
                        timeCount--
                        try {
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                        }
                        if (timeCount <= 0) {
                            runOnUiThread {
                                Toast.makeText(
                                    applicationContext,
                                    "No Connection...",
                                    Toast.LENGTH_LONG
                                ).show()
                                startButton.isEnabled = true
                                startButton.textSize = 16f
                                startButton.text = "Restart Test"
                            }
                            getSpeedTestHostsHandler = null
                            return
                        }
                    }

                    //Find closest server
                    val mapKey = getSpeedTestHostsHandler!!.getMapKey()
                    val mapValue = getSpeedTestHostsHandler!!.getMapValue()
                    val selfLat = getSpeedTestHostsHandler!!.getSelfLat()
                    val selfLon = getSpeedTestHostsHandler!!.getSelfLon()
                    var tmp = 19349458.0
                    var dist = 0.0
                    var findServerIndex = 0
                    for (index in mapKey.keys) {
                        if (tempBlackList!!.contains(mapValue[index]!![5])) {
                            continue
                        }
                        val source = Location("Source")
                        source.latitude = selfLat
                        source.longitude = selfLon
                        val ls = mapValue[index]!!
                        val dest = Location("Dest")
                        dest.latitude = ls[0].toDouble()
                        dest.longitude = ls[1].toDouble()
                        val distance = source.distanceTo(dest).toDouble()
                        if (tmp > distance) {
                            tmp = distance
                            dist = distance
                            findServerIndex = index
                        }
                    }
                    val testAddr = mapKey[findServerIndex]!!
                        .replace("http://", "https://")
                    val info = mapValue[findServerIndex]
                    val distance = dist
                    if (info == null) {
                        runOnUiThread {
                            startButton.textSize = 12f
                            startButton.text =
                                "There was a problem in getting Host Location. Try again later."
                        }
                        return
                    }
                    runOnUiThread {
                        startButton.textSize = 13f
                        startButton.text = String.format(
                            "Host Location: %s [Distance: %s km]",
                            info[2],
                            DecimalFormat("#.##").format(distance / 1000)
                        )
                    }


                    //Reset value, graphics
                    runOnUiThread {
                        pingTextView.text = "0 ms"

                        downloadTextView.text = "0 Mbps"

                        uploadTextView.text = "0 Mbps"

                    }
                    val pingRateList: MutableList<Double> =
                        ArrayList()
                    val downloadRateList: MutableList<Double> =
                        ArrayList()
                    val uploadRateList: MutableList<Double> =
                        ArrayList()
                    var pingTestStarted = false
                    var pingTestFinished = false
                    var downloadTestStarted = false
                    var downloadTestFinished = false
                    var uploadTestStarted = false
                    var uploadTestFinished = false

                    val pingTest = PingTest(info[6].replace(":8080", ""), 3)
                    val downloadTest = HttpDownloadTest(
                        testAddr.replace(
                            testAddr.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()[testAddr.split("/".toRegex())
                                .dropLastWhile { it.isEmpty() }
                                .toTypedArray().size - 1], ""))
                    val uploadTest = HttpUploadTest(testAddr)

// Tests
                    while (true) {
                        if (!pingTestStarted) {
                            pingTest.start()
                            pingTestStarted = true
                        }
                        if (pingTestFinished && !downloadTestStarted) {
                            downloadTest.start()
                            downloadTestStarted = true
                        }
                        if (downloadTestFinished && !uploadTestStarted) {
                            uploadTest.start()
                            uploadTestStarted = true
                        }

                        // Ping Test
                        if (pingTest.isFinished()) {
                            pingTestFinished = true
                            // Failure
                            if (pingTest.avgRtt == 0.0) {
                                println("Ping error...")
                            } else {
                                // Success
                                runOnUiThread {
                                    pingTextView.text = dec.format(pingTest.avgRtt) + " ms"
                                }
                            }
                        } else {
                            pingRateList.add(pingTest.instantRtt)
                            runOnUiThread {
                                pingTextView.text = dec.format(pingTest.instantRtt) + " ms"
                            }
                        }

                        //Download Test
                        if (pingTestFinished) {
                            if (downloadTestFinished) {
                                //Failure
                                if (downloadTest.getFinalDownloadRate() == 0.0) {
                                    println("Download error...")
                                } else {
                                    //Success
                                    runOnUiThread {
                                        downloadTextView.text =
                                            dec.format(downloadTest.getFinalDownloadRate()) + " Mbps"
                                    }
                                }
                            } else {
                                //Calc position
                                val downloadRate = downloadTest.getInstantDownloadRate()
                                downloadRateList.add(downloadRate)
                                position = getPositionByRate(downloadRate)
                                runOnUiThread {
                                    rotate = RotateAnimation(
                                        lastPosition.toFloat(),
                                        position.toFloat(),
                                        Animation.RELATIVE_TO_SELF,
                                        0.5f,
                                        Animation.RELATIVE_TO_SELF,
                                        0.5f
                                    )
                                    rotate!!.interpolator = LinearInterpolator()
                                    rotate!!.duration = 100
                                    barImageView.startAnimation(rotate)
                                    downloadTextView.text =
                                        dec.format(downloadTest.getInstantDownloadRate()) + " Mbps"
                                }
                                lastPosition =
                                    position
                            }
                        }

                        //Upload Test
                        if (downloadTestFinished) {
                            if (uploadTestFinished) {
                                //Failure
                                if (uploadTest.finalUploadRate == 0.0) {
                                    println("Upload error...")
                                } else {
                                    //Success
                                    runOnUiThread {
                                        uploadTextView.text =
                                            dec.format(uploadTest.finalUploadRate) + " Mbps"
                                    }
                                }
                            } else {
                                //Calc position
                                val uploadRate = uploadTest.instantUploadRate
                                uploadRateList.add(uploadRate)
                                position = getPositionByRate(uploadRate)
                                runOnUiThread {
                                    rotate = RotateAnimation(
                                        lastPosition.toFloat(),
                                        position.toFloat(),
                                        Animation.RELATIVE_TO_SELF,
                                        0.5f,
                                        Animation.RELATIVE_TO_SELF,
                                        0.5f
                                    )
                                    rotate!!.interpolator = LinearInterpolator()
                                    rotate!!.duration = 100
                                    barImageView.startAnimation(rotate)
                                    uploadTextView.text =
                                        dec.format(uploadTest.instantUploadRate) + " Mbps"
                                }
                                lastPosition =
                                    position
                            }
                        }

                        //Test bitti
                        if (pingTestFinished && downloadTestFinished && uploadTest.isFinished) {
                            break
                        }
                        if (pingTest.isFinished()) {
                            pingTestFinished = true
                        }
                        if (downloadTest.isFinished()) {
                            downloadTestFinished = true
                        }
                        if (uploadTest.isFinished) {
                            uploadTestFinished = true
                        }
                        if (pingTestStarted && !pingTestFinished) {
                            try {
                                Thread.sleep(300)
                            } catch (e: InterruptedException) {
                            }
                        } else {
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                            }
                        }
                    }
// Inside the runOnUiThread block where the download and upload test results are available
                    runOnUiThread {
                        // Set the text of TextViews to display the download and upload results
                        downloadResultTextView.text =
                            "Download Result: ${dec.format(downloadTest.getFinalDownloadRate())} Mbps"
                        uploadResultTextView.text =
                            "Upload Result: ${dec.format(uploadTest.finalUploadRate)} Mbps"
                        pingResultTextView.text = "Ping Result: ${dec.format(pingTest.avgRtt)}  ms"
                        if (downloadTest.getFinalDownloadRate() > 2.5) {
                            downloadResultTextView.visibility = View.VISIBLE
                            downloadResultTextView.text = "Download Result: Passed"
                            downloadResultTextView.setTextColor(ContextCompat.getColor(applicationContext, R.color.Color2)) // Green for Excellent
                        } else {
                            downloadResultTextView.visibility = View.VISIBLE
                            downloadResultTextView.text = "Download Result: Failed"
                            downloadResultTextView.setTextColor(ContextCompat.getColor(applicationContext, R.color.Color5)) // Red for Poor
                        }
                        if (uploadTest.finalUploadRate > 0.5) {
                            uploadResultTextView.visibility = View.VISIBLE
                            uploadResultTextView.text = "Upload Result: Passed"
                            uploadResultTextView.setTextColor(ContextCompat.getColor(applicationContext, R.color.Color2)) // Green for Excellent
                        } else {
                            uploadResultTextView.visibility = View.VISIBLE
                            uploadResultTextView.text = "Upload Result: Failed"
                            uploadResultTextView.setTextColor(ContextCompat.getColor(applicationContext, R.color.Color5)) // Red for Poor
                        }
                        if (pingTest.avgRtt < 150) {
                            pingResultTextView.visibility = View.VISIBLE
                            pingResultTextView.text = "Ping Result: Passed"
                            pingResultTextView.setTextColor(ContextCompat.getColor(applicationContext, R.color.Color2)) // Green for Excellent
                        } else {
                            pingResultTextView.visibility = View.VISIBLE
                            pingResultTextView.text = "Ping Result: Failed"
                            pingResultTextView.setTextColor(ContextCompat.getColor(applicationContext, R.color.Color5)) // Red for Poor
                        }
                    }


                    //Thread bitiminde button yeniden aktif ediliyor
                    runOnUiThread {
                        startButton.isEnabled = true
                        startButton.textSize = 16f
                        startButton.text = "Restart Test"
                    }
                }
            }).start()
        }
    }

    fun getPositionByRate(rate: Double): Int {
        if (rate <= 1) {
            return (rate * 30).toInt()
        } else if (rate <= 10) {
            return (rate * 6).toInt() + 30
        } else if (rate <= 30) {
            return ((rate - 10) * 3).toInt() + 90
        } else if (rate <= 50) {
            return ((rate - 30) * 1.5).toInt() + 150
        } else if (rate <= 100) {
            return ((rate - 50) * 1.2).toInt() + 180
        }
        return 0
    }

    companion object {
        var position = 0
        var lastPosition = 0
    }
}






