package com.example

import java.io.BufferedReader
import java.io.InputStreamReader


/**
 * @author erdigurbuz
 */

class PingTest(serverIpAddress: String, pingTryCount: Int) : Thread() {
    var result = HashMap<String, Any>()
    var server = ""
    var count: Int
    @JvmField
    var instantRtt = 0.0
    @JvmField
    var avgRtt = 0.0
    var finished = false
    var started = false

    init {
        server = serverIpAddress
        count = pingTryCount
    }

    fun getAvgRtt(): Double {
        return avgRtt
    }

    fun getInstantRtt(): Double {
        return instantRtt
    }

    fun isFinished(): Boolean {
        return finished
    }

    override fun run() {
        try {
            val ps = ProcessBuilder("ping", "-c $count", server)
            ps.redirectErrorStream(true)
            val pr = ps.start()
            val `in` = BufferedReader(InputStreamReader(pr.inputStream))
            var line: String
            while (`in`.readLine().also { line = it } != null) {
                if (line.contains("icmp_seq")) {
                    instantRtt = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray().size - 2].replace("time=", "").toDouble()
                }
                if (line.startsWith("rtt ")) {
                    avgRtt = line.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[4].toDouble()
                    break
                }
            }
            pr.waitFor()
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finished = true
    }
}








