package com.xingtai.mqtt.listener;

/**
 * MQTT connection listening
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttConnectListener
 * @time 2023/3/10 14:21
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public interface MqttConnectListener {
    /**
     * Connection successful
     */
    void onSuccess();

    /**
     * Connection failed
     *
     * @param exception
     */
    void onFailure(Throwable exception);

    /**
     * Connection loss exception callback
     *
     * @param exception
     */
    default void connectionLost(Throwable exception) {
    }

    /**
     * Connection completed
     *
     * @param reconnect
     * @param serverURI
     */
    default void connectComplete(boolean reconnect, String serverURI) {

    }
}
