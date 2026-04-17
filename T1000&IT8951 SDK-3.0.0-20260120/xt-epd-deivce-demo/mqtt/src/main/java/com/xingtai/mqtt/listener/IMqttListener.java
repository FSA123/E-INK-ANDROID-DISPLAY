package com.xingtai.mqtt.listener;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;



public interface IMqttListener {

    /**
     * Connection successful, start subscribing to topic
     *
     * @param asyncActionToken
     */
    void connectSuccess(IMqttToken asyncActionToken);

    /**
     * connection failed
     * @param asyncActionToken
     * @param exception
     */
    void connectFailed(IMqttToken asyncActionToken, Throwable exception);


    /**
     * Received message
     *
     * @param topic   topic
     * @param message message
     * @param qos     qos
     */
    void messageArrived(String topic, String message, int qos);

    /**
     * Transmission completed
     *
     * @param token
     */
    void deliveryComplete(IMqttDeliveryToken token);

    /**
     * Connection lost
     *
     * @param cause Thrown exception information
     */
    void connectionLost(Throwable cause);

    /**
     * Connection completed
     * @param reconnect
     * @param serverURI
     */
    void connectComplete(boolean reconnect, String serverURI);
}
