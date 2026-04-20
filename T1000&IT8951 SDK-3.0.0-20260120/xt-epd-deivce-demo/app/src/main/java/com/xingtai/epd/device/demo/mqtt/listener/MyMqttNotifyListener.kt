package com.xingtai.epd.device.demo.mqtt.listener

import com.sjl.util.LogUtils
import com.xingtai.epd.device.demo.mqtt.entity.BusTextRequest
import com.xingtai.epd.device.demo.mqtt.entity.ImgRequest
import com.xingtai.mqtt.listener.MqttNotifyListener
import com.xingtai.epd.device.demo.mqtt.util.BusTextRenderUtils
import com.xingtai.epd.device.demo.mqtt.util.ImgDownloadUtils
import com.xingtai.epd.device.demo.util.GsonUtils
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
        try {
            val rawAction = JSONObject(message).optString("action", null)
            when (rawAction) {
                BusTextRequest.ACTION_SEND_BUS_INFO -> handleBusTextRequest(message)
                ImgRequest.ACTION_SEND_IMG -> handleImgRequest(message)
                else -> LogUtils.w("Unsupported or missing action in MQTT message: $rawAction")
            }
        } catch (e: Exception) {
            LogUtils.e("json parsing exception", e)
        }
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
}
