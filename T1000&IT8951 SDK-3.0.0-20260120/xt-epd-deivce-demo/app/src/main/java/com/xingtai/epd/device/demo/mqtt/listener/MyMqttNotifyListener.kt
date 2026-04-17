package com.xingtai.epd.device.demo.mqtt.listener

import com.sjl.util.LogUtils
import com.xingtai.epd.device.demo.mqtt.entity.ImgRequest
import com.xingtai.mqtt.listener.MqttNotifyListener
import com.xingtai.epd.device.demo.mqtt.util.ImgDownloadUtils
import com.xingtai.epd.device.demo.util.GsonUtils


/**
 * MQTT message notification listening
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
        //Here, the image sent by the server is simulated and downloaded
        try {
            val imgRequest = GsonUtils.fromJson(message, ImgRequest::class.java)
            imgRequest?.let {
                ImgDownloadUtils.download(it, object :
                    com.xingtai.epd.device.demo.mqtt.listener.DownloadCallback {
                    override fun onSuccess() {}
                    override fun onError(e: Throwable) {}
                })
            }
        } catch (e: Exception) {
            LogUtils.e("json parsing exception",e)
        }
    }
}
