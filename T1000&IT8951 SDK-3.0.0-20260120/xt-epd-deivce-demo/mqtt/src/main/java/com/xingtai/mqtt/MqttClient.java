package com.xingtai.mqtt;

import android.content.Context;
import android.util.Log;

import com.xingtai.mqtt.entity.MqttPublishRequest;
import com.xingtai.mqtt.listener.IMqttListener;
import com.xingtai.mqtt.listener.MqttPublishListener;
import com.xingtai.mqtt.listener.MqttSubscribeListener;
import com.xingtai.mqtt.util.MqttUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.File;
import java.io.FilenameFilter;

/**
 * MQTT client
 *
 * @author song
 */
public class MqttClient {

    private final String TAG = "MqttClient";

    private boolean canDoConnect = true;

    private MqttAndroidClient mqttAndroidClient;
    private MqttDefaultFilePersistence  mqttDefaultFilePersistence;
    private File externalFilesDir;

    private MqttConnectOptions conOpt;

    private Context context;
    private String serverUrl;
    private String userName;
    private String passWord;
    private String clientId;
    private int timeOut;
    private int keepAliveInterval;
    private boolean cleanSession;
    private boolean autoReconnect;
    private IMqttListener mqttListener;



    private MqttClient(Builder builder) {
        this.context = builder.context;
        this.serverUrl = builder.serverUrl;
        this.userName = builder.userName;
        this.passWord = builder.passWord;
        this.clientId = builder.clientId;
        this.timeOut = builder.timeOut;
        this.keepAliveInterval = builder.keepAliveInterval;
        this.cleanSession = builder.cleanSession;
        this.autoReconnect = builder.autoReconnect;
        init();
    }





    public static final class Builder {

        private Context context;
        private String serverUrl;
        private String userName = "admin";
        private String passWord = "admin";
        private String clientId;
        private int timeOut = 10;
        private int keepAliveInterval = 20;
        private boolean cleanSession = false;
        private boolean autoReconnect = false;

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder passWord(String passWord) {
            this.passWord = passWord;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder timeOut(int timeOut) {
            this.timeOut = timeOut;
            return this;
        }

        public Builder keepAliveInterval(int keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }


        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        public Builder cleanSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
            return this;
        }

        public MqttClient bulid(Context context) {
            this.context = context;
            return new MqttClient(this);
        }
    }


    private void init() {
        // Set the persistent storage path to an external file directory
        externalFilesDir = context.getExternalFilesDir("MqttConnection");
        mqttDefaultFilePersistence = new MqttDefaultFilePersistence(externalFilesDir.getAbsolutePath());
        // Server address (protocol+address+port number)
        mqttAndroidClient = new MqttAndroidClient(context, serverUrl, clientId,mqttDefaultFilePersistence);
        // Set up MQTT listening and receive messages
        mqttAndroidClient.setCallback(mqttCallback);
        conOpt = new MqttConnectOptions();
        // Clear session
        conOpt.setCleanSession(cleanSession);
        // Set timeout time in seconds
        conOpt.setConnectionTimeout(timeOut);
        // Heartbeat packet transmission interval, unit: seconds
        conOpt.setKeepAliveInterval(keepAliveInterval);
        // User name
        conOpt.setUserName(userName);
        // Password
        conOpt.setPassword(passWord.toCharArray());
        //Set automatic reconnect
        conOpt.setAutomaticReconnect(autoReconnect);
        //Allow multiple messages to be sent simultaneously
        conOpt.setMaxInflight(1000);
    }

    /**
     * Close the client
     */
    public void close() {
        try {
            //Close connection
            mqttAndroidClient.close();
            //Cancel registration of resources
            mqttAndroidClient.unregisterResources();
            //Disconnecting and not calling will result in MQTT callback not callback for the next connection
            mqttAndroidClient.disconnect();
            Log.i(TAG,"close client");
        } catch (Exception e) {
            Log.e(TAG,"Close exception",e);
        }
    }

    /**
     * Connect to MQTT server
     */
    public void connect(IMqttListener mqttListener) {
        this.mqttListener = mqttListener;
        try {
            if (canDoConnect && !mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect(conOpt,null, iMqttActionListener);
            }
        } catch (Exception e) {
            Log.e(TAG,"Connection exception",e);
            if (this.mqttListener != null) {
                this.mqttListener.connectFailed(null, new Exception("Connection exception"));
            }
        }
    }

    /**
     * Publish message
     * @param mqttPublishRequest
     * @param publishListener
     */
    public void publish(MqttPublishRequest mqttPublishRequest, MqttPublishListener publishListener) {

        try {
            mqttAndroidClient.publish(mqttPublishRequest.topic, mqttPublishRequest.payload, mqttPublishRequest.qos, mqttPublishRequest.retained, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (publishListener != null){
                        publishListener.onSuccess();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (publishListener != null){
                        publishListener.onFailure(exception);
                    }
                }
            });
        } catch (Exception e) {
            if (publishListener != null){
                publishListener.onFailure(e);
            }
        }
    }


    /**
     * Subscription topics
     *
     * @param topics
     * @param qos
     */
    public void subscribe(String[] topics, int[] qos, MqttSubscribeListener mqttSubscribeListener) {
        if (!isConnected()){
            return;
        }
        try {
            Log.i(TAG, "execute subscribe -- qos = " + qos.toString());
            mqttAndroidClient.subscribe(topics, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (mqttSubscribeListener != null){
                        mqttSubscribeListener.onSuccess();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (mqttSubscribeListener != null){
                        mqttSubscribeListener.onFailure(exception);
                    }
                }
            });
        } catch (Exception e) {
            if (mqttSubscribeListener != null){
                mqttSubscribeListener.onFailure(e);
            }
        }
    }

    /**
     * Unsubscribe topics
     * @param topics
     */
    public void unsubscribe(String[] topics) {
        if (!isConnected()){
            return;
        }
        try {
            mqttAndroidClient.unsubscribe(topics);
        } catch (Exception e) {
            Log.e(TAG,"unsubscribe",e);
        }
    }
    /**
     * Disconnect
     */
    public void disconnect(){
        if (!isConnected()){
            return;
        }
        try {
            mqttAndroidClient.disconnect();
        } catch (Exception e) {
            Log.e(TAG,"disconnect",e);
        }
    }

    /**
     * Disconnect
     */
    public void disconnect(IMqttActionListener iMqttActionListener){
        if (!isConnected()){
            iMqttActionListener.onSuccess(null);
            return;
        }
        try {
            mqttAndroidClient.disconnect(null,iMqttActionListener);
        } catch (Exception e) {
            Log.e(TAG,"disconnect",e);
            iMqttActionListener.onFailure(null, new Exception("Abnormal disconnection"));

        }
    }


    /**
     * Determine if the connection has been disconnected
     */
    public boolean isConnected(){
        try {
            return mqttAndroidClient.isConnected();
        } catch (Exception e) {
            Log.e(TAG,"isConnected",e);
        }
        return false;
    }


    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {


        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.i(TAG, "mqtt connect success ");
            if (mqttListener != null) {
                mqttListener.connectSuccess(asyncActionToken);
            }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.i(TAG, "mqtt connect failed ");
            if (mqttListener != null) {
                mqttListener.connectFailed(asyncActionToken, exception);
            }
        }
    };


    private MqttCallbackExtended mqttCallback = new MqttCallbackExtended() {



        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            String msgContent = new String(message.getPayload());
            String detailLog = topic + ";qos:" + message.getQos() + ";retained:" + message.isRetained();
            Log.i(TAG, "messageArrived:" + msgContent);
            Log.i(TAG, detailLog);
            if (mqttListener != null) {
                mqttListener.messageArrived(topic, msgContent, message.getQos());
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            if (mqttListener != null) {
                mqttListener.deliveryComplete(token);
            }
            Log.i(TAG, "deliveryComplete");
        }

        @Override
        public void connectionLost(Throwable cause) {
            if (mqttListener != null) {
                mqttListener.connectionLost(cause);
            }
            Log.e(TAG, "connectionLost",cause);

        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (mqttListener != null) {
                mqttListener.connectComplete(reconnect,serverURI);
            }
        }
    };


    /**
     * Delete client persistence information
     *
     * @return
     */
    public boolean deletePersistence() throws Exception{
        Log.w(TAG, "deletePersistence clientId:" + clientId);
        if (externalFilesDir == null || clientId == null) {
            return false;
        }
        File[] files = externalFilesDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(clientId);
            }
        });
        if (files == null || files.length == 0){
            return false;
        }
        for (File file:files){
            MqttUtils.deleteDirectory(file);
        }

        return true;
    }
}