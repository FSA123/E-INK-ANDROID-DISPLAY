package com.xingtai.mqtt.listener;


import com.xingtai.mqtt.entity.MqttResponse;

/**
 *  MQTT message subscribe listening
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttSubscribeListener
 * @time 2023/3/10 13:58
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public interface MqttSubscribeListener {

    void onSuccess();

    void onFailure(Throwable exception);
}
