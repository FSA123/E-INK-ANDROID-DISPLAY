package com.xingtai.mqtt.entity;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttSubscribeRequest
 * @time 2023/3/10 13:35
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public class MqttSubscribeRequest {
    public String topic;
    public boolean isSubscribe;
    public int qos = 0;
}
