package com.xingtai.epd.device.demo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Process
import com.sjl.util.LogUtils
import com.sjl.util.LogWriter
import com.xingtai.mqtt.MqttHelper
import com.xingtai.mqtt.entity.MqttPublishRequest
import com.xingtai.mqtt.entity.MqttSubscribeRequest
import com.xingtai.mqtt.listener.MqttConnectListener
import com.xingtai.mqtt.listener.MqttPublishListener
import com.xingtai.mqtt.listener.MqttSubscribeListener
import com.xingtai.epd.device.demo.app.MyApplication.Companion.getContext
import com.xingtai.epd.device.demo.app.MyApplication.Companion.getMainThreadExecutor
import com.xingtai.epd.device.demo.mqtt.listener.MyMqttNotifyListener
import com.xingtai.epd.device.demo.service.listener.ServiceListener
import com.xingtai.epd.device.demo.util.DeviceUtils
import com.xingtai.epd.device.demo.util.SpUtils.getMqttConfig


/**
 * MQTT service
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MyMqttService
 * @time 2023/3/10 14:30
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
class MyMqttService : BaseService() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val CHANNEL_ID = "xt-device"
    private val CHANNEL_NANE = "mqtt"
    private var notificationManager: NotificationManager? = null
    private fun initNotice() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val mChannel: NotificationChannel
        //Foreground service needs to add this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NANE, NotificationManager.IMPORTANCE_MIN)
            notificationManager!!.createNotificationChannel(mChannel)
            val notification = Notification.Builder(this, CHANNEL_ID)
                .build()
            startForeground(100, notification)
        }
    }

    override fun onCreate() {
        super.onCreate()
        initNotice()
        val pid = Process.myPid()
        LogUtils.i("Create MQTT service,pid：$pid")
        //Monitor MQTT messages
        MqttHelper.getInstance().registerOnPushListener(MyMqttNotifyListener())
        initMqtt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @Synchronized
    private fun initMqtt() {
        val connected = MqttHelper.getInstance().isConnected
        if (connected) {
            MqttHelper.getInstance().close()
        }
        val context = getContext()
        val mqttHelper = MqttHelper.getInstance()
        val mqttConfig = getMqttConfig()
        mqttConfig?.let {
            val clientId = mqttConfig.clientId
            LogWriter.i("Start MQTT service connection,clientId:$clientId")
            val username = it.username
            val pwd = it.password
            mqttHelper.initMqttClient(context,
                it.ip,
                it.port,
                clientId,
                username,
                pwd,
                object : MqttConnectListener {
                    override fun onSuccess() {
                        LogWriter.i("MQTT connection successful")
                    }

                    override fun onFailure(exception: Throwable) {
                        LogWriter.e("Connection exception,username:" + it.username + ",pwd:" + it.password,
                            exception)
                        if (exception is NullPointerException) {
                            val b = mqttHelper.deletePersistence()
                            LogWriter.i("MQTT connection null exception occurred, execute configuration deletion，b:$b")
                        }
                    }

                    override fun connectionLost(exception: Throwable) {
                        LogWriter.e("Connection lost", exception)
                    }

                    override fun connectComplete(reconnect: Boolean, serverURI: String) {
                        if (reconnect) {
                            LogWriter.w("MQTT reconnection successful,serverURI:$serverURI")
                        } else {
                            LogWriter.i("MQTT successfully connected for the first time,serverURI:$serverURI")
                        }
                        subscribe(it.subTopic)
                    }
                })
        }

    }

    private fun subscribe(subTopic: String?) {
        if (subTopic == null){
            return
        }
        val subscribeRequest = MqttSubscribeRequest()
        val deviceId = DeviceUtils.deviceId
        subscribeRequest.qos = QOS
        subscribeRequest.topic = subTopic + deviceId
        MqttHelper.getInstance().subscribe(subscribeRequest, object : MqttSubscribeListener {
            override fun onSuccess() {
                LogWriter.i("Subscription message successful")
            }

            override fun onFailure(exception: Throwable) {
                LogWriter.e("Subscription message failed", exception)
            }
        })
        pullData()
        uploadData()
    }

    /**
     * Every time the device is online, pull the latest resources
     */
    private fun pullData() {}

    /**
     * Report data,for example: device information
     */
    private fun uploadData() {}
    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    override fun release() {
        MqttHelper.getInstance().unregisterOnPushListener()
        MqttHelper.getInstance().close()
        // Stop foreground service and delete notifications
        stopForeground(true)
        LogWriter.w("Stop MQTT service")
        getMainThreadExecutor().post(Runnable {
            serviceListener?.onDestroy()
            serviceListener = null
        })
    }

    companion object {
        /**
         * Send a message to the server's topic, defined by the user
         */
        private const val TOPIC_PUBLISH = "deviceToServer/"

        /**
         * Message service quality
         */
        const val QOS = 1
        private var serviceListener: ServiceListener? = null

        /**
         * Start service
         *
         * @param context
         */
        fun startService(context: Context) {
            val intent = Intent(context, MyMqttService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop service
         *
         * @param context
         */
        fun stopService(context: Context) {
            context.stopService(Intent(context, MyMqttService::class.java))
        }

        /**
         *  Stop service
         *
         * @param context
         * @param serviceListener
         */
        fun stopService(context: Context, serviceListener: ServiceListener) {
            context.stopService(Intent(context, MyMqttService::class.java))
            Companion.serviceListener = serviceListener
        }

        /**
         * Publish a message to MQTT server
         *
         * @param data
         */
        @JvmOverloads
        fun publish(data: String,
                    mqttPublishListener: MqttPublishListener? = object : MqttPublishListener {
                        override fun onSuccess() {
                            LogUtils.i("Message successfully published")
                        }

                        override fun onFailure(exception: Throwable) {
                            LogUtils.e("Failed to publish message", exception)
                        }
                    }) {
            val mqttPublishRequest = MqttPublishRequest()
            val deviceId = DeviceUtils.deviceId
            mqttPublishRequest.topic = TOPIC_PUBLISH + deviceId
            mqttPublishRequest.qos = QOS
            mqttPublishRequest.payload = data.toByteArray()
            LogUtils.i("publish content:$data")
            MqttHelper.getInstance().publish(mqttPublishRequest, mqttPublishListener)
        }
    }
}
