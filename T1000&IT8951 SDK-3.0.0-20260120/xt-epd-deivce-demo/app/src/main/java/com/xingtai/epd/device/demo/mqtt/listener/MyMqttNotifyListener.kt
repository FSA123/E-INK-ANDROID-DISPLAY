package com.xingtai.epd.device.demo.mqtt.listener

import android.util.Log
import com.xingtai.epd.device.demo.mqtt.entity.BusLine
import com.xingtai.epd.device.demo.mqtt.entity.BusTextRequest
import com.xingtai.epd.device.demo.mqtt.entity.ImgRequest
import com.xingtai.mqtt.listener.MqttNotifyListener
import com.xingtai.epd.device.demo.mqtt.util.BusTextRenderUtils
import com.xingtai.epd.device.demo.mqtt.util.ImgDownloadUtils
import com.xingtai.epd.device.demo.util.GsonUtils
import org.json.JSONArray
import org.json.JSONObject


/**
 * MQTT message notification listening.
 *
 * Routes incoming messages by their `action` field:
 *  - [BusTextRequest.ACTION_SEND_BUS_INFO] → renders a 5-band bus-arrival text image locally.
 *  - [ImgRequest.ACTION_SEND_IMG]          → downloads an image from a URL (legacy flow).
 *  - anything else                         → logged and ignored.
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttNotifyListener
 * @time 2023/3/10 11:36
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
class MyMqttNotifyListener : MqttNotifyListener {

    companion object {
        private const val TAG = "SMARTTRANS"
    }

    override fun onNotify(topic: String, message: String, qos: Int) {
        Log.i(TAG, "STEP1: onNotify called. topic=$topic  qos=$qos  payloadLength=${message.length}")
        Log.i(TAG, "STEP1: raw payload start: '${message.take(120).replace("\n", "\\n")}'")
        val payload = message.trim()
        Log.i(TAG, "STEP2: trimmed payload starts with '[' = ${payload.startsWith("[")}")
        try {
            if (payload.startsWith("[")) {
                Log.i(TAG, "STEP3: routing to handleArrayBusTextRequest")
                handleArrayBusTextRequest(payload)
                return
            }
            val jsonObject = JSONObject(payload)
            val rawAction = jsonObject.optString("action", "")
            Log.i(TAG, "STEP3: object payload, action='$rawAction'")
            when (rawAction) {
                BusTextRequest.ACTION_SEND_BUS_INFO -> handleBusTextRequest(payload)
                ImgRequest.ACTION_SEND_IMG -> handleImgRequest(payload)
                else -> Log.w(TAG, "STEP3: unsupported action: $rawAction")
            }
        } catch (e: Exception) {
            Log.e(TAG, "STEP_ERR: exception during parsing", e)
        }
    }

    private fun handleArrayBusTextRequest(message: String) {
        Log.i(TAG, "STEP4: entering parseBusTextRequestFromArrayPayload")
        val request = parseBusTextRequestFromArrayPayload(message)
        if (request == null) {
            Log.e(TAG, "STEP4_ERR: parseBusTextRequestFromArrayPayload returned null")
            return
        }
        if (request.lines.isNullOrEmpty()) {
            Log.e(TAG, "STEP4_ERR: parsed request has no lines")
            return
        }
        Log.i(TAG, "STEP5: parsed ${request.lines!!.size} lines — screen will show:")
        request.lines!!.forEachIndexed { i, line ->
            Log.i(TAG, "  [${i + 1}] RouteName='${line.destinationStopName}'   ArrivalTime='${line.arrivalTime}'")
        }
        Log.i(TAG, "STEP6: calling BusTextRenderUtils.render()")
        BusTextRenderUtils.render(request)
    }

    private fun handleBusTextRequest(message: String) {
        val request = GsonUtils.fromJson(message, BusTextRequest::class.java)
        if (request == null) {
            Log.e(TAG, "STEP4_ERR: failed to parse BusTextRequest")
            return
        }
        if (request.lines == null) {
            Log.e(TAG, "STEP4_ERR: BusTextRequest missing required 'lines' field")
            return
        }
        BusTextRenderUtils.render(request)
    }

    private fun handleImgRequest(message: String) {
        val imgRequest = GsonUtils.fromJson(message, ImgRequest::class.java)
        imgRequest?.let {
            ImgDownloadUtils.download(it, object : DownloadCallback {
                override fun onSuccess() {}
                override fun onError(e: Throwable) {}
            })
        }
    }

    private fun parseBusTextRequestFromArrayPayload(message: String): BusTextRequest? {
        val root = JSONArray(message)
        Log.i(TAG, "PARSE: root array length=${root.length()}")

        var busInfoObject: JSONObject? = null
        for (i in 0 until root.length()) {
            val item = root.optJSONObject(i) ?: continue
            val id = item.optInt("id", -1)
            Log.i(TAG, "PARSE: item[$i] id=$id")
            if (id == 1) {
                busInfoObject = item
                break
            }
        }

        if (busInfoObject == null) {
            Log.e(TAG, "PARSE_ERR: no object with id=1 found in root array")
            return null
        }

        val data = busInfoObject.optJSONObject("data")
        if (data == null) {
            Log.e(TAG, "PARSE_ERR: object with id=1 has no 'data' field")
            return null
        }

        val rawElements = data.optJSONArray("rawElements") ?: data.optJSONArray("rawElement")
        if (rawElements == null) {
            Log.e(TAG, "PARSE_ERR: 'data' has no 'rawElements' field. data keys=${data.keys().asSequence().toList()}")
            return null
        }

        val stationName = busInfoObject.optString("name", "").trim()
        Log.i(TAG, "PARSE: stationName='$stationName'")

        Log.i(TAG, "PARSE: rawElements count=${rawElements.length()}, will use first ${minOf(rawElements.length(), 5)}")
        val lines = mutableListOf<BusLine>()
        val limit = minOf(rawElements.length(), 5)
        for (i in 0 until limit) {
            val rawElement = rawElements.optJSONObject(i) ?: continue
            val stopName = rawElement.optString("toStopName", "").trim()
            val time = rawElement.optString("displayedTime", "").trim()
            Log.i(TAG, "PARSE: rawElement[$i] toStopName='$stopName'  displayedTime='$time'")
            lines.add(BusLine().apply {
                destinationStopName = stopName
                arrivalTime = time
            })
        }

        return BusTextRequest().apply {
            action = BusTextRequest.ACTION_SEND_BUS_INFO
            this.stationName = stationName
            this.lines = lines
        }
    }
}