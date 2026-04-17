package com.xingtai.mqtt;

import android.content.Context;
import android.util.Log;

import com.xingtai.mqtt.entity.MqttPublishRequest;
import com.xingtai.mqtt.entity.MqttSubscribeRequest;
import com.xingtai.mqtt.listener.IMqttListener;
import com.xingtai.mqtt.listener.MqttConnectListener;
import com.xingtai.mqtt.listener.MqttNotifyListener;
import com.xingtai.mqtt.listener.MqttPublishListener;
import com.xingtai.mqtt.listener.MqttSubscribeListener;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * MQTT connection help class
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MqttHelper
 * @time 2023/3/9 18:41
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
public class MqttHelper {
    private static final String TAG = "MqttHelper";

    private static MqttHelper mqttHelper;
    private MqttClient mqttClient;
    private MqttNotifyListener mqttNotifyListener;
    private MqttConnectListener mqttConnectListener;
    public static final int RECONNECT_RETRY_COUNT = 3;

    private MqttHelper(){}

    public static MqttHelper getInstance(){
        if (mqttHelper == null){
            synchronized (MqttHelper.class){
                if (mqttHelper == null){
                    mqttHelper = new MqttHelper();
                }
            }
        }
        return mqttHelper;
    }


    public  void initMqttClient(Context context, String ip, int port, String clientId, String name, String passWord, MqttConnectListener mqttConnectListener){
        if (isConnected()) {
            if (mqttConnectListener != null){
                mqttConnectListener.onSuccess();
            }
            return;
        }
        this.mqttConnectListener = mqttConnectListener;
        mqttClient = new MqttClient.Builder()
                //Set automatic reconnection
                .autoReconnect(true)
                //Set clear callback session，  true(false) Do not accept (accept) push messages sent by the server before
                .cleanSession(true)
                //Unique identifier, ensuring that each device is unique is sufficient. It is recommended to use IMEI or the unique ID of the current device
                .clientId(clientId)
                .userName(name)
                .passWord(passWord)
                //MQTT server address, format for example：tcp://iot.eclipse.org:1883
                .serverUrl("tcp://" + ip + ":" + port)
                //Default sending interval for heartbeat packets
                .keepAliveInterval(30)
                //Connection timeout
                .timeOut(30)
                //Suggest using the context of the application
                .bulid(context);
        deletePersistence();
        connect();
    }

    /**
     * Connect to Mqtt server
     */
    private void connect() {
        mqttClient.connect(new IMqttListener() {
            @Override
            public void connectSuccess(IMqttToken asyncActionToken) {
                if (mqttConnectListener != null){
                    mqttConnectListener.onSuccess();
                }
                notifyCdt();

            }

            @Override
            public void connectFailed(IMqttToken asyncActionToken, Throwable exception) {
                notifyCdt();
                if (exception instanceof MqttSecurityException){
                    MqttSecurityException mqttSecurityException = (MqttSecurityException) exception;
                    int reasonCode = mqttSecurityException.getReasonCode();
                    if (reasonCode == 5){
                        Log.e(TAG,"Unauthorized connection or device not registered",mqttSecurityException);
                        return;
                    }
                }
                if (mqttConnectListener != null){
                    mqttConnectListener.onFailure(exception);
                }

            }

            @Override
            public void messageArrived(String topic, String message, int qos) {
                if (mqttNotifyListener != null){
                    mqttNotifyListener.onNotify(topic, message, qos);
                }
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }

            @Override
            public void connectionLost(Throwable cause) {
                if (cause != null){
                    Log.e(TAG,"connectionLost",cause);
                }
                if (mqttConnectListener != null){
                    mqttConnectListener.connectionLost(cause);
                }
            }


            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (mqttConnectListener != null){
                    mqttConnectListener.connectComplete(reconnect,serverURI);
                }
            }
        });
    }




    /**
     * Determine if the service is connected
     */
    public boolean isConnected() {
        if (mqttClient == null) {
            return false;
        }
        return mqttClient.isConnected();
    }

    /**
     * Publish message
     * @param mqttPublishRequest
     * @param mqttPublishListener
     */
    public void publish(MqttPublishRequest mqttPublishRequest, MqttPublishListener mqttPublishListener) {
        if (mqttClient == null){
            return;
        }
        mqttClient.publish(mqttPublishRequest,mqttPublishListener);
    }

    /**
     * Subscribe topic
     * @param mqttSubscribeRequest
     * @param mqttSubscribeRequest
     */
    public void subscribe(MqttSubscribeRequest mqttSubscribeRequest, MqttSubscribeListener mqttSubscribeListener) {
        if (mqttClient == null){
            return;
        }
        String[] topics = new String[]{mqttSubscribeRequest.topic};
        int[] qoss = new int[]{mqttSubscribeRequest.qos};
        mqttClient.subscribe(topics,qoss,mqttSubscribeListener);
    }

    /**
     * Unsubscribe topic
     * @param mqttSubscribeRequest
     * @param mqttSubscribeRequest
     */
    public void unsubscribe(MqttSubscribeRequest mqttSubscribeRequest) {
        if (mqttClient == null){
            return;
        }
        String[] topics = new String[]{mqttSubscribeRequest.topic};
        mqttClient.unsubscribe(topics);
    }

    /**
     * Disconnect
     */
    public void disconnect() {
        if (mqttClient == null){
            return;
        }
        mqttClient.disconnect();
    }
    private CountDownLatch reconnectCdt;

    /**
     * Reconnect
     */
    public void reconnect() {
        if (mqttClient == null){
            return;
        }
        mqttClient.disconnect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                performReconnect();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                performReconnect();
            }
        });


    }

    private void performReconnect() {
        try {
            reconnectCdt = new CountDownLatch(1);
            connect();
            try {
                reconnectCdt.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {

            }

            Log.i(TAG,"This reconnection is complete");
        } catch (Exception e) {
            Log.e(TAG,"reconnect",e);
        }finally {
            reconnectCdt = null;
        }
    }

    private void notifyCdt() {
        if (reconnectCdt == null){
            return;
        }
        long count = reconnectCdt.getCount();
        if (count > 0) {
            reconnectCdt.countDown();
        }
    }
    /**
     * Close the client
     */
    public void close() {
        if (mqttClient == null){
            return;
        }
        this.mqttConnectListener = null;
        mqttClient.close();
    }

    /**
     * Delete the persistence information of the client ID
     *
     * @return
     */
    public boolean deletePersistence() {
        if (mqttClient == null){
            return false;
        }
        try {
            return mqttClient.deletePersistence();
        } catch (Exception e) {
            Log.e(TAG, "deletePersistence", e);
        }
        return false;
    }

    public  void registerOnPushListener(MqttNotifyListener mqttNotifyListener){
        this.mqttNotifyListener = mqttNotifyListener;
    }

    public  void unregisterOnPushListener(){
        mqttNotifyListener = null;
    }

}
