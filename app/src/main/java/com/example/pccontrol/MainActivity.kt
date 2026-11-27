package com.example.pccontrol

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val pcIp = "192.168.1.6"
    private val macAddress = "10:FF:E0:DA:2E:A4"
    private val port = 8787

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        val btnOn: Button = findViewById(R.id.btnOn)
        val btnOff: Button = findViewById(R.id.btnOff)
        val btnSteam: Button = findViewById(R.id.btnSteam)

        btnOn.setOnClickListener {
            updateStatus("جاري إرسال إشارة Wake-on-LAN (ON)...")
            thread {
                try {
                    sendWakeOnLan(macAddress)
                    updateStatus("تم إرسال إشارة التشغيل بنجاح")
                } catch (e: Exception) {
                    updateStatus("خطأ في إرسال Wake-on-LAN: ${e.message}")
                }
            }
        }

        btnOff.setOnClickListener {
            updateStatus("جاري إرسال أمر إغلاق الجهاز (OFF)...")
            sendHttpCommand("/shutdown")
        }

        btnSteam.setOnClickListener {
            updateStatus("جاري إرسال أمر تشغيل Steam...")
            sendHttpCommand("/start-steam")
        }
    }

    private fun sendWakeOnLan(macStr: String) {
        val cleanMac = macStr.replace(":", "").replace("-", "")
        val macBytes = ByteArray(6) { i ->
            cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }

        val packetBytes = ByteArray(6 + 16 * macBytes.size)
        for (i in 0..5) {
            packetBytes[i] = 0xFF.toByte()
        }
        for (i in 6 until packetBytes.size step macBytes.size) {
            System.arraycopy(macBytes, 0, packetBytes, i, macBytes.size)
        }

        val broadcastAddress = InetAddress.getByName("255.255.255.255")
        val socket = DatagramSocket()
        socket.broadcast = true
        val packet = DatagramPacket(packetBytes, packetBytes.size, broadcastAddress, 9)
        socket.send(packet)
        socket.close()
    }

    private fun sendHttpCommand(endpoint: String) {
        thread {
            try {
                val url = URL("http://$pcIp:$port$endpoint")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                val code = conn.responseCode
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                updateStatus("الرد ($code): $body")
            } catch (e: Exception) {
                updateStatus("تعذر الوصول للجهاز (تأكد أنه شغال وعلى نفس الشبكة)")
            }
        }
    }

    private fun updateStatus(msg: String) {
        runOnUiThread {
            tvStatus.text = msg
        }
    }
}
