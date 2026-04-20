package com.xingtai.epd.device.demo.mqtt.listener

import com.sjl.util.LogUtils
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
    override fun onNotify(topic: String, message: String, qos: Int) {
        LogUtils.i("topic:$topic,receive message:$message,qos:$qos")
        val payload = message.trim()
        try {
            if (payload.startsWith("[")) {
                handleArrayBusTextRequest(payload)
                return
            }
            val rawAction = JSONObject(payload).optString("action", null)
            when (rawAction) {
                BusTextRequest.ACTION_SEND_BUS_INFO -> handleBusTextRequest(payload)
                ImgRequest.ACTION_SEND_IMG -> handleImgRequest(payload)
                else -> LogUtils.w("Unsupported or missing action in MQTT message: $rawAction")
            }
        } catch (e: Exception) {
            LogUtils.e("json parsing exception", e)
        }
    }

    private fun handleArrayBusTextRequest(message: String) {
        val request = parseBusTextRequestFromArrayPayload(message)
        if (request?.lines.isNullOrEmpty()) {
            LogUtils.e("BusTextRequest: failed to parse array payload or extract lines from data.rawElements")
            return
        }
        BusTextRenderUtils.render(request)
    }

    private fun handleBusTextRequest(message: String) {
        val request = GsonUtils.fromJson(message, BusTextRequest::class.java)
        if (request == null) {
            LogUtils.e("BusTextRenderUtils: failed to parse BusTextRequest")
            return
        }
        if (request.lines == null) {
            LogUtils.e("BusTextRequest: missing required 'lines' field")
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
        var busInfoObject: JSONObject? = null
        for (i in 0 until root.length()) {
            val item = root.optJSONObject(i) ?: continue
            if (item.optInt("id", -1) == 1) {
                busInfoObject = item
                break
            }
        }

        val rawElements = busInfoObject
            ?.optJSONObject("data")
            ?.optJSONArray("rawElements")
            ?: return null

        val lines = mutableListOf<BusLine>()
        val limit = minOf(rawElements.length(), 5)
        for (i in 0 until limit) {
            val rawElement = rawElements.optJSONObject(i) ?: continue
            val destinationStopName = rawElement.optString("toStopName", "").trim()
            val displayedTime = rawElement.optString("displayedTime", "").trim()
            if (destinationStopName.isEmpty() || displayedTime.isEmpty()) {
                continue
            }

            val line = BusLine().apply {
                this.destinationStopName = destinationStopName
                arrivalTime = displayedTime
            }
            lines.add(line)
        }

        return BusTextRequest().apply {
            action = BusTextRequest.ACTION_SEND_BUS_INFO
            this.lines = lines
        }
    }
}
