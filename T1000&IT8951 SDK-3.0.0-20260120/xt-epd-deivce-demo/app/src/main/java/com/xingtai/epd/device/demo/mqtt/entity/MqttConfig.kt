package com.xingtai.epd.device.demo.mqtt.entity

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttConfig
 * @time 2024/9/2 13:49
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
class MqttConfig {
    /**
     * client ID
     */
    var clientId: String? = null

    /**
     * ip
     */
    var ip: String? = null

    /**
     * port
     */
    var port = -1

    /**
     * username
     */
    var username: String? = null

    /**
     * password
     */
    var password: String? = null

    /**
     * subscribe topic,topic for subscribing to server messages
     */
    var subTopic: String? = null
    override fun toString(): String {
        return "MqttConfig{" +
                "clientId='" + clientId + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", subTopic='" + subTopic + '\'' +
                '}'
    }
}