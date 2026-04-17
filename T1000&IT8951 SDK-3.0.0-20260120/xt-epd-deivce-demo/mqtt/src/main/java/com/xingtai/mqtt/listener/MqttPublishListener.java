package com.xingtai.mqtt.listener;


import com.xingtai.mqtt.entity.MqttResponse;

/**
 * MQTT message publish listening
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttPublishListener
 * @time 2023/3/10 13:41
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public interface MqttPublishListener {

    void onSuccess();

    void onFailure(Throwable exception);
}
