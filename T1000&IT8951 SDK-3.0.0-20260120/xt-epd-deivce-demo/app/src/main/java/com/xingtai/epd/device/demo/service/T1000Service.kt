package com.xingtai.epd.device.demo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.sjl.deviceconnector.device.usb.UsbHelper
import com.sjl.deviceconnector.listener.UsbPlugListener
import com.sjl.util.LogWriter
import com.xingtai.device.mcu.Mcu
import com.xingtai.device.mcu.McuKeyCode
import com.xingtai.device.mcu.listener.BatteryChangeListener
import com.xingtai.device.mcu.listener.McuKeyListener
import com.xingtai.epd.device.demo.app.AppConstant
import com.xingtai.epd.device.demo.app.MyApplication.Companion.getExecutor
import com.xingtai.epd.device.demo.app.MyApplication.Companion.getMainThreadExecutor
import com.xingtai.epd.device.demo.mcu.McuHelper.Companion.instance
import com.xingtai.epd.device.demo.receiver.DeviceBootReceiver
import com.xingtai.epd.device.demo.receiver.MediaReceiver
import com.xingtai.epd.device.demo.service.listener.ServiceListener
import com.xingtai.epd.device.demo.t1000.AbstractT1000Helper
import com.xingtai.epd.device.demo.t1000.T1000HelperFactory
import com.xingtai.epd.device.demo.t1000.entity.EpdImage
import com.xingtai.epd.device.demo.t1000.listener.ImageDisplayChangeListener
import com.xingtai.epd.device.demo.t1000.listener.OnEpdImageListener
import com.xingtai.epd.device.demo.t1000.listener.T1000InitListener
import com.xingtai.epd.device.demo.t1000.util.UsbFlashDiskUtils
import com.xingtai.epd.device.demo.util.CollectionUtils
import java.util.Timer
import java.util.TimerTask

/**
 * T1000 service
 *
 * @author Kelly
 * @version 1.0.0
 * @filename T1000Service
 * @time 2023/3/31 16:21
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
class T1000Service : BaseService() {
    private var imageProducerTimer: Timer? = null
    private var stop = false
    private var mediaReceiver: MediaReceiver? = null
    private var currentImgIndex = 0
    private var initialDelay: Long = 0
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val CHANNEL_ID = "xt-device"
    private val CHANNEL_NANE = "xt-t1000"
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
            startForeground(101, notification)

        }
    }

    override fun onCreate() {
        super.onCreate()
        initNotice()
        stop = false
        abstractT1000 = T1000HelperFactory.instance
        LogWriter.i("Create T1000 service:$this")
        UsbHelper.getInstance().setUsbPlugListener(object : UsbPlugListener {
            @RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            override fun onAttached(usbDevice: UsbDevice) {
                LogWriter.i("Device attached,name：" + usbDevice.deviceName + ",vendorId:" + usbDevice.vendorId + ",productId:" + usbDevice.productId)
                if (UsbFlashDiskUtils.isUsbFlashDisk(usbDevice)) {
                    LogWriter.i("This device is a USB flash disk")
                } else {
                    abstractT1000?.let {
                        it.init(usbDevice, object : T1000InitListener {

                            override fun onSuccess(isLast: Boolean) {
                                if (isLast) {
                                    it.startDisplay()
                                }

                            }
                        })
                    }

                }
            }

            @RequiresApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            override fun onDetached(usbDevice: UsbDevice) {
                LogWriter.w("Device detached,name：" + usbDevice.toString() + ",vendorId:" + usbDevice.vendorId + ",productId:" + usbDevice.productId)

                abstractT1000?.let {
                    val effectiveDevice = it.isEffectiveDevice(usbDevice)
                    if (effectiveDevice) {

                        it.closeOnDetached(usbDevice)
                        it.isUsbDetached = true

                    }
                }

            }
        })
        UsbHelper.getInstance().registerReceiver()

        initMcu()

        initResourcesPlayMode()
    }

    /**
     * Initialize MCU
     */
    private fun initMcu() {
        val mcuHelper = instance()
        mcuHelper.setBatteryChangeListener(BatteryChangeListener { charge, mV, batterySoc ->
            if (charge < 0 || mV <= 0) {
                LogWriter.w("Battery level changed, charge:$charge,mV:$mV,batterySoc:$batterySoc")
                return@BatteryChangeListener
            }

            val tempSoc = mcuHelper.oldSoc
            val soc = mcuHelper.getCompatSoc(charge, mV, batterySoc)
            if (charge == Mcu.BATTERY_UNCHARGED) {
                LogWriter.i("Not charging, battery level changed：" + mV + "mV,before soc:" + tempSoc + "%,after soc:" + soc + "%")
            } else if (charge == Mcu.BATTERY_CHARGING) {
                LogWriter.i("Charging, battery level changed：" + mV + "mV,before soc:" + tempSoc + "%,after soc:" + soc + "%")
            } else {
                LogWriter.i("====Fully charged, battery level changed：" + mV + "mV,soc:" + soc + "%")
            }
            //print mcu info
            mcuHelper.mcuInfo
        })
        mcuHelper.setMcuKeyListener(McuKeyListener { keyCode, keyDelayFlag, det ->
            val keyDelayTime = mcuHelper.getKeyDelay(keyDelayFlag)
            LogWriter.i("Trigger key：" + keyCode + ",keyDelay:" + if (keyDelayFlag == McuKeyCode.KEY_DELAY_SHORT_PRESS) "Short press" else "Long press" + keyDelayTime + "s")
            if (keyCode == McuKeyCode.KEY_UP) {
                if (keyDelayFlag == McuKeyCode.KEY_DELAY_SHORT_PRESS) {
                } else if (keyDelayFlag == McuKeyCode.KEY_DELAY_LONG_PRESS_3) {
                } else if (keyDelayFlag == McuKeyCode.KEY_DELAY_LONG_PRESS_5) {
                } else if (keyDelayFlag >= McuKeyCode.KEY_DELAY_LONG_PRESS_10) {
                }
            } else if (keyCode == McuKeyCode.KEY_DOWN) {
            } else if (keyCode == McuKeyCode.KEY_POWER) {
            }
        })
        val enableMcu = mcuHelper.isEnableMcu
        if (enableMcu) {
            getExecutor().execute {
                mcuHelper.initMcu()
            }

        }
    }

    /**
     * Initialize resource play mode
     */
    private fun initResourcesPlayMode() {
        initialDelay = AppConstant.IMAGE_PRODUCER_INTERVAL_TIME.toLong()
        initData()
        //Start producer timer
        startInterval()

        initMediaReceiver()
    }

    private fun initData() {
        buildEpdImages(object : OnEpdImageListener<List<EpdImage>> {
            override fun onEpdImageChange(epdImages: List<EpdImage>) {
                val usbDevices: List<UsbDevice> = AbstractT1000Helper.Companion.listDevices()
                if (CollectionUtils.isNotEmpty(usbDevices)) {
                    LogWriter.i("======The T1000 device exists======,usbDevices:" + usbDevices.size)
                    for (i in usbDevices.indices) {
                        val usbDevice = usbDevices[i]
                        abstractT1000?.let {
                            it.init(usbDevice, object : T1000InitListener {
                                override fun onSuccess(isLast: Boolean) {
                                    if (isLast) {
                                        //必须在初始化后赋值
                                        if (CollectionUtils.isNotEmpty(epdImages)) {
                                            it.initEpdImage(epdImages)
                                        } else {
                                            LogWriter.i("There are no available resources for the current task...")
                                        }
                                        it.startDisplay()
                                    }


                                }

                            })
                        }

                    }
                }else{
                    abstractT1000?.startDisplay()
                }
                abstractT1000?.let {
                    it.imageDisplayChangeListener = object : ImageDisplayChangeListener {
                        override fun onSuccess(epdImage: EpdImage) {

                        }

                        override fun onFail(e: Exception) {

                        }
                    }
                }

                LogWriter.i("rebootFlag:" + DeviceBootReceiver.rebootFlag)
            }

        })
    }

    private fun buildEpdImages(onEpdImageListener: OnEpdImageListener<List<EpdImage>>) {
        getExecutor().execute(Runnable { //TODO:When the service starts, read the local image
            val epdImages: List<EpdImage> = ArrayList()
            onEpdImageListener.onEpdImageChange(epdImages)
        })
    }

    /**
     * Initialize media broadcast (listen to USB flash disk insertion and removal)
     */
    private fun initMediaReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED)
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT)
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)

        // We need to set URL matching tags for the filter and use file to match implicit intent, otherwise we cannot receive USB plug and unplug events
        intentFilter.addDataScheme("file")
        mediaReceiver = MediaReceiver()
        registerReceiver(mediaReceiver, intentFilter)
    }

    /**
     * Start displaying
     */
    @Synchronized
    private fun startDisplay() {
        if (stop) {
            LogWriter.e("The T1000 service has been destroyed")
            cancelInterval()
            return
        }
        abstractT1000?.run {
            if (isFull) {
                return
            }
            if (isBusy) {
                return
            }
            if (isSleep) {
                return
            }
            if (!isConsumerRunning()) {
                LogWriter.w("====Detected that the consumption thread has stopped running and started restarting...====")
                startDisplay()
                return
            }
            if (currentImgIndex >= 0) {
                updateCurrentImgIndex(currentImgIndex + 1)
                currentPlayIndex = currentImgIndex
                currentImgIndex = -1
            }
            val nextEpdImage = nextEpdImage
                ?: //            LogUtils.w("======No pictures to display======");
                return
            val file = nextEpdImage.file
            file?.let {
                if (it.exists() && it.isFile && it.canRead()) {
                    //Meet the display conditions
                    val existImage = isExistImage(nextEpdImage)
                    if (!existImage) {
                        val b = addImage(nextEpdImage)
                        if (b) {
                            autoIncrementCurrentPlayIndex()
                        }
                    }
                } else {
                    //Remove invalid files
                    remove(nextEpdImage)
                    LogWriter.w("Not satisfied with transferring images:" + it.absolutePath)
                    autoIncrementCurrentPlayIndex()
                }
            }

        }

    }

    /**
     * Next round of playback
     */
    private fun nextPlay() {
        startDisplay()
    }

    /**
     * Start timer
     */
    private fun startInterval() {
        cancelInterval()
        imageProducerTimer = Timer()
        val intervalTime = AppConstant.IMAGE_PRODUCER_INTERVAL_TIME
        imageProducerTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                nextPlay()
            }
        }, initialDelay, intervalTime.toLong())
    }

    /**
     * Cancel timer
     */
    private fun cancelInterval() {
        imageProducerTimer?.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        if (mediaReceiver != null) {
            unregisterReceiver(mediaReceiver)
            mediaReceiver = null
        }
        LogWriter.w("Stop T1000 service")
    }

    override fun release() {
        stop = true
        cancelInterval()
        UsbHelper.getInstance().unregisterReceiver()
        abstractT1000?.let {
            it.requestSleepInterrupted()
            it.disconnect()
        }
        instance().close()
        getMainThreadExecutor().post(Runnable {
            serviceListener?.onDestroy()
            serviceListener = null
        })
    }

    companion object {
        val TAG = T1000Service::class.java.simpleName
        private var serviceListener: ServiceListener? = null

        /**
         * Start service
         *
         * @param context
         */
        fun startService(context: Context) {
            val intent = Intent(context, T1000Service::class.java)
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
            val t1000Service: BaseService = ServiceManager[TAG]
                ?: return
            getExecutor().execute(Runnable {
                t1000Service.release()
                context.stopService(Intent(context, T1000Service::class.java))
            })
        }

        /**
         * Stop service
         *
         * @param context
         * @param serviceListener
         */
        fun stopService(context: Context, serviceListener: ServiceListener) {
            val t1000Service: BaseService = ServiceManager[TAG]
                ?: return
            Companion.serviceListener = serviceListener
            getExecutor().execute(Runnable {
                t1000Service.release()
                context.stopService(Intent(context, T1000Service::class.java))
            })
        }

        var abstractT1000: AbstractT1000Helper? = null
    }
}
