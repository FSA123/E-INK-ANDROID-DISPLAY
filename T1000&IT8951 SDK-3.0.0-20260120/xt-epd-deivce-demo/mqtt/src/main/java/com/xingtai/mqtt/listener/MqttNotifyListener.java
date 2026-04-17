package com.xingtai.mqtt.listener;


/**
 * MQTT message notification listening
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttNotifyListener
 * @time 2023/3/10 11:36
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public interface MqttNotifyListener {

    /**
     * Received message notification
     * @param topic
     * @param message
     * @param qos
     */
    void onNotify(String topic, String message, int qos);
}
