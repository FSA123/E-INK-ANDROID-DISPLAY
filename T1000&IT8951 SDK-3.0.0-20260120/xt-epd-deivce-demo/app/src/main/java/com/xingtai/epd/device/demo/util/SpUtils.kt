package com.xingtai.epd.device.demo.util


import android.text.TextUtils
import com.xingtai.epd.device.demo.app.AppConstant
import com.xingtai.epd.device.demo.mcu.entity.SerialConfig
import com.xingtai.epd.device.demo.mqtt.entity.MqttConfig


/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename SpUtils
 * @time 2021/1/21 11:07
 * @copyright(C) 2021 song
 */
object SpUtils {

    private var ph = MMKVHelper


    fun getMqttConfig(): MqttConfig? {
        val json: String = ph.decodeString(AppConstant.SpParams.MQTT_CONFIG)
        return if (TextUtils.isEmpty(json)) {
            return null
        } else {
            return GsonUtils.fromJson(json, MqttConfig::class.java)
        }
    }



    fun saveMqttConfig(mqttConfig: MqttConfig) {
        if (mqttConfig != null) {
            ph.encode(AppConstant.SpParams.MQTT_CONFIG, GsonUtils.toJson(mqttConfig))
        }
    }


    fun getSerialConfig(): SerialConfig? {
        val json: String = ph.decodeString(AppConstant.SpParams.SERIAL_CONFIG)
        return if (TextUtils.isEmpty(json)) {
            return null
        } else {
            return GsonUtils.fromJson(json, SerialConfig::class.java)
        }
    }


    fun saveSerialConfig(serialConfig: SerialConfig) {
        ph.encode(AppConstant.SpParams.SERIAL_CONFIG, GsonUtils.toJson(serialConfig))
    }
}

