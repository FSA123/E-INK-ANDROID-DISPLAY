package com.xingtai.mqtt.entity;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttPublishRequest
 * @time 2023/3/10 11:46
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public class MqttPublishRequest {
    /**
     * Topic for publishing messages
     */
    public String topic;
    /**
     * Byte array of message
     */
    public byte[] payload;
    /**
     * The service quality of providing messages can be 0, 1, or 2. It is recommended that the themes configured on both the server and client be consistent
     * 0 At most once, it means that only one push message will be sent, and it doesn't matter whether it is received or not
     * 1 At least once, guaranteed to receive messages, but not necessarily only one
     * 2 Multiple times, ensure to receive only one message
     */
    public int qos;
    /**
     * Should the last message after disconnection be retained on the server
     */
    public boolean retained;

}
