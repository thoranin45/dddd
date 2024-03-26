package com.example

import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory


/**
 * @author erdigurbuz
 */

class HttpDownloadTest(fileURL: String) : Thread() {
    var fileURL = ""
    var startTime: Long = 0
    var endTime: Long = 0
    var downloadElapsedTime = 0.0
    @JvmField var downloadedByte = 0
    @JvmField var finalDownloadRate = 0.0
    var finished = false
    @JvmField var instantDownloadRate = 0.0
    var timeout = 8
    var httpsConn: HttpsURLConnection? = null

    // เนื่องจากใช้ @JvmField แล้วไม่มีเมทอด getter หรือ setter ถูกสร้างขึ้นโดยอัตโนมัติ

    init {
        this.fileURL = fileURL
    }
    private fun round(value: Double, places: Int): Double {
        require(places >= 0)
        var bd: BigDecimal
        bd = try {
            BigDecimal(value)
        } catch (ex: Exception) {
            return 0.0
        }
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    fun getInstantDownloadRate(): Double {
        return instantDownloadRate
    }

    fun setInstantDownloadRate(downloadedByte: Int, elapsedTime: Double) {
        if (downloadedByte >= 0) {
            instantDownloadRate = round((downloadedByte * 8 / (1000 * 1000) / elapsedTime), 2)
        } else {
            instantDownloadRate = 0.0
        }
    }

    fun getFinalDownloadRate(): Double {
        return round(finalDownloadRate, 2)
    }

    fun isFinished(): Boolean {
        return finished
    }

    override fun run() {
        var url: URL? = null
        downloadedByte = 0
        var responseCode = 0
        val fileUrls: MutableList<String> = ArrayList()
        fileUrls.add(fileURL + "random4000x4000.jpg")
        fileUrls.add(fileURL + "random3000x3000.jpg")
        startTime = System.currentTimeMillis()
        outer@ for (link in fileUrls) {
            try {
                url = URL(link)
                httpsConn = url.openConnection() as HttpsURLConnection
                httpsConn!!.sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                httpsConn!!.hostnameVerifier = HostnameVerifier { hostname, session -> true }
                httpsConn!!.connect()
                responseCode = httpsConn!!.responseCode
            } catch (ex: Exception) {
                ex.printStackTrace()
                break@outer
            }
            try {
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val buffer = ByteArray(10240)
                    val inputStream = httpsConn!!.inputStream
                    var len = 0
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        downloadedByte += len
                        endTime = System.currentTimeMillis()
                        downloadElapsedTime = (endTime - startTime) / 1000.0
                        setInstantDownloadRate(downloadedByte, downloadElapsedTime)
                        if (downloadElapsedTime >= timeout) {
                            break@outer
                        }
                    }
                    inputStream.close()
                    httpsConn!!.disconnect()
                } else {
                    println("Link not found...")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        endTime = System.currentTimeMillis()
        downloadElapsedTime = (endTime - startTime) / 1000.0
        finalDownloadRate = downloadedByte * 8 / (1000 * 1000.0) / downloadElapsedTime
        finished = true
    }
}