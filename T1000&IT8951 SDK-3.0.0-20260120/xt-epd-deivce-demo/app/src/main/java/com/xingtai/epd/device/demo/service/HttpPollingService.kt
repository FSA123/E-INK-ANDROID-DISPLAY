package com.xingtai.epd.device.demo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.sjl.util.LogWriter
import com.xingtai.epd.device.demo.mqtt.entity.BusLine
import com.xingtai.epd.device.demo.mqtt.entity.BusTextRequest
import com.xingtai.epd.device.demo.mqtt.util.BusTextRenderUtils
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Foreground service that polls the SmartTrans HTTP API every 30 seconds and
 * renders updated bus-arrival data onto the EPD screen.
 *
 * Replaces the MQTT-based MyMqttService for the SmartTrans deployment.
 */
class HttpPollingService : BaseService() {

    private val executor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "http-poll").also { it.isDaemon = true }
    }
    @Volatile private var pollFuture: ScheduledFuture<*>? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initForeground()
        Log.i(TAG, "HttpPollingService started — polling every ${POLL_INTERVAL_S}s")
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    override fun release() {
        pollFuture?.cancel(false)
        executor.shutdownNow()
        stopForeground(true)
        Log.i(TAG, "HttpPollingService stopped")
    }

    // ── Foreground notification ───────────────────────────────────────────────

    private fun initForeground() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Bus Polling", NotificationManager.IMPORTANCE_MIN)
            )
            startForeground(101, Notification.Builder(this, CHANNEL_ID).build())
        }
    }

    // ── Polling loop ─────────────────────────────────────────────────────────

    private fun startPolling() {
        pollFuture = executor.scheduleWithFixedDelay(
            ::poll, 0L, POLL_INTERVAL_S, TimeUnit.SECONDS
        )
    }

    private fun poll() {
        try {
            Log.i(TAG, "Polling $API_URL")
            val json = httpGet(API_URL)
            if (json.isNullOrBlank()) {
                Log.w(TAG, "Empty response from server")
                return
            }
            val request = parseArrayPayload(json)
            if (request == null) {
                Log.e(TAG, "Failed to parse API response")
                return
            }
            Log.i(TAG, "Parsed ${request.lines?.size ?: 0} bus lines for '${request.stationName}'")
            BusTextRenderUtils.render(request)
        } catch (e: Exception) {
            LogWriter.e("HttpPollingService poll error", e)
        }
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private fun httpGet(urlString: String): String? {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout    = READ_TIMEOUT_MS
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP $code from $urlString")
                return null
            }
            BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // ── JSON parsing (same array format as the legacy MQTT payload) ───────────

    private fun parseArrayPayload(json: String): BusTextRequest? {
        val root = JSONArray(json.trim())
        Log.d(TAG, "Root array length=${root.length()}")

        var busInfoObject = run {
            for (i in 0 until root.length()) {
                val item = root.optJSONObject(i) ?: continue
                if (item.optInt("id", -1) == 1) return@run item
            }
            null
        }

        if (busInfoObject == null) {
            Log.e(TAG, "No object with id=1 in response")
            return null
        }

        val data = busInfoObject.optJSONObject("data") ?: run {
            Log.e(TAG, "id=1 object has no 'data' field")
            return null
        }

        val rawElements = data.optJSONArray("rawElements") ?: data.optJSONArray("rawElement") ?: run {
            Log.e(TAG, "'data' has no rawElements. keys=${data.keys().asSequence().toList()}")
            return null
        }

        val stationName = busInfoObject.optString("name", "").trim()
        val limit = minOf(rawElements.length(), 5)
        val lines = mutableListOf<BusLine>()
        for (i in 0 until limit) {
            val el = rawElements.optJSONObject(i) ?: continue
            lines.add(BusLine().apply {
                destinationStopName = el.optString("toStopName", "").trim()
                arrivalTime         = el.optString("displayedTime", "").trim()
            })
        }

        Log.i(TAG, "Station='$stationName' lines=$limit")
        return BusTextRequest().apply {
            action           = BusTextRequest.ACTION_SEND_BUS_INFO
            this.stationName = stationName
            this.lines       = lines
        }
    }

    companion object {
        private const val TAG              = "HttpPolling"
        private const val CHANNEL_ID       = "xt-http-poll"
        private const val POLL_INTERVAL_S  = 30L
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS    = 10_000
        const val API_URL =
            "https://www.smarttrans.ro/focsani/api/api-get-baza-dev--e-paper.php?stid=4"

        fun startService(context: Context) {
            val intent = Intent(context, HttpPollingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, HttpPollingService::class.java))
        }
    }
}
