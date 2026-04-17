package com.xingtai.epd.device.demo.t1000

import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.os.SystemClock
import android.util.Log
import com.sjl.deviceconnector.ErrorCode
import com.sjl.deviceconnector.listener.UsbPermissionListener
import com.sjl.util.LogUtils
import com.sjl.util.LogWriter
import com.xingtai.device.t1000.TConConstant.DisplayMode.MODE_INIT
import com.xingtai.device.t1000.entity.ScreenType
import com.xingtai.device.t1000.entity.T1000UsbDevice
import com.xingtai.device.t1000.exception.DeviceConnectException
import com.xingtai.device.t1000.exception.DeviceLoseException
import com.xingtai.device.t1000.exception.DisplayException
import com.xingtai.device.t1000.exception.DisplayInterruptException
import com.xingtai.device.t1000.exception.LoadException
import com.xingtai.device.t1000.exception.ParamsException
import com.xingtai.device.t1000.open.T1000Device
import com.xingtai.epd.device.demo.mcu.McuHelper.Companion.instance
import com.xingtai.epd.device.demo.t1000.entity.EpdImage
import com.xingtai.epd.device.demo.t1000.listener.T1000DisplayListener
import com.xingtai.epd.device.demo.t1000.listener.T1000InitListener
import com.xingtai.epd.device.demo.util.AppConfigUtils
import com.xingtai.epd.device.demo.util.MyAppUtils
import com.xingtai.epd.device.demo.util.PollUtils
import java.io.File
import java.util.concurrent.Executors

/**
 * T1000 helper class (single device), used to control image transmission
 *
 * @author Kelly
 * @version 1.0.0
 * @filename T1000Helper
 * @time 2023/3/31 16:16
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
class T1000Helper : AbstractT1000Helper() {
    private var t1000Device: T1000Device? = null

    /**
     * Cache local image file, no need to process images every time, does not support bitmap caching,this is a memory cache
     */
    var cacheSize = 20 * 1024 * 1024
    /**
     * Display thread pool
     */
    private val displayExecutor = Executors.newSingleThreadExecutor()
    private val obj = Object()


    override fun init(usbDevice: UsbDevice, t1000InitListener: T1000InitListener) {
        if (usbDevice == null) {
            return
        }
        if (t1000InitListener == null) {
            return
        }
        if (mSupportedDevices.contains(T1000UsbDevice(usbDevice.vendorId,
                usbDevice.productId))) {
            //When powered on, the connection will be processed internally to prevent duplicate processing
            if (instance().isEnableMcu && instance().isT1000Power) {
                return
            }
            requestPermission(usbDevice, object : UsbPermissionListener {
                override fun onGranted(usbDevice: UsbDevice) {
                    disconnect()
                    isBusy = false
                    //Just initialized and not connected
                    t1000Device = T1000Device(usbDevice)
                    screenType?.let {
                        t1000Device?.setType(it.type)
                    }
                    t1000InitListener.onSuccess(true)
                }

                override fun onDenied(usbDevice: UsbDevice) {
                    LogWriter.e("T1000 device permission application rejected")
                    t1000InitListener.onFail(Exception("T1000 device permission application rejected"))
                }
            })
        } else {
            t1000InitListener.onFail(Exception("T1000 device not found"))
        }
    }

    override fun open(): Int {
        return t1000Device?.open() ?: ErrorCode.ERROR_FAIL
    }

    override val state: Int
        get() = t1000Device?.state ?: ErrorCode.ERROR_NOT_CONNECTED

    override fun close() {
        t1000Device?.let {
            LogWriter.i("Device close...")
            it.close()
        }
    }


    override fun display(epdImage: EpdImage, t1000DisplayListener: T1000DisplayListener) {
        if (isBusy) {
            return
        }
        isBusy = true
        this.t1000DisplayListener = t1000DisplayListener

        displayExecutor.execute(object : Runnable {
            override fun run() {
                try {
                    var ret: Int = state
                    if (ret != ErrorCode.ERROR_OK) {
                        close()
                        val device: UsbDevice? = AbstractT1000Helper.device
                        ret = if (device != null) {
                            requestPermission(device, object : UsbPermissionListener {
                                override fun onGranted(usbDevice: UsbDevice) {
                                    LogWriter.i("USB authorization successful")
                                }

                                override fun onDenied(usbDevice: UsbDevice) {}
                            })
                            open()
                        } else {
                            callFail(DeviceLoseException("T1000 device does not exist, image loading failed"))
                            return
                        }
                    }
                    if (ret != ErrorCode.ERROR_OK) {
                        callFail(DeviceConnectException("Connection to T1000 device failed：$ret"))
                        return
                    }
                    //Enable file cache
                    t1000Device?.setEnableCache(true,cacheSize)

                    if (t1000DeviceInfo == null) {
                        t1000DeviceInfo = t1000Device!!.t1000DeviceInfo
                    }
                    val formatType = epdImage.formatType
                    val startX = epdImage.startX
                    val startY = epdImage.startY
                    val screenType = AppConfigUtils.screenType

                    var width = screenType.width
                    var height = screenType.height

                    var displayMode = epdImage.displayMode
                    var enableDither = false
                    val colorsScreen = ScreenType.isColorsScreen(screenType)
                    var filePath:String? = null
                    var bytes:ByteArray? = null

                    if (formatType == EpdImage.FORMAT_TYPE_FILE){
                        val file = epdImage.file
                        filePath = file?.absolutePath
                        if (file == null || !file.exists()) {
                            callFail(ParamsException("The image file does not exist：$filePath"))
                            return
                        }
                        val options = BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeFile(filePath, options)
                        width = options.outWidth
                        height = options.outHeight

                        if (colorsScreen) {
                            enableDither = true
                        }
                    }else if (formatType == EpdImage.FORMAT_TYPE_BYTE){
                        bytes = epdImage.bytes
                    }else{
                        throw Exception("The image data format is not supported：$formatType")
                    }

                    if (screenType == ScreenType.SCREEN_13_3_MONOCHROME){
                        t1000Device!!.setPower(true)
                    }

                    if (!colorsScreen && instance().lastT1000PowerOff) {
                        initScreen(startX, startY, width, height)
                        instance().lastT1000PowerOff = false
                    }

                    var startTime = System.currentTimeMillis()
                    LogWriter.i("imgCache:"+t1000Device?.imgCache.toString())
                    LogWriter.i("current sensor temperature:" + t1000Device?.temperature)
                    if (formatType == EpdImage.FORMAT_TYPE_FILE){
                        ret = t1000Device!!.loadImage(filePath,
                            enableDither,
                            startX,
                            startY,
                            width,
                            height,
                            displayMode)
                    }else{
                        ret = t1000Device!!.loadImage(bytes, startX, startY, width, height)
                    }

                    if (ret != ErrorCode.ERROR_OK) {
                        callFail(LoadException("Image loading failed：$ret"))
                        return
                    }
                    LogWriter.i("Image load time：${(System.currentTimeMillis() - startTime)/1000.0}s")
                    startTime = System.currentTimeMillis()
                    ret = t1000Device!!.display(startX, startY, width, height, displayMode)
                    if (ret != ErrorCode.ERROR_OK) {
                        callFail(DisplayException("Image display failed：$ret"))
                        return
                    }
                    if (screenType == ScreenType.SCREEN_13_3_MONOCHROME){
                        t1000Device!!.setPower(false)
                    }
                    if (formatType == EpdImage.FORMAT_TYPE_BYTE){
                        epdImage.file?.let {
                            if (it.exists()){
                                it.delete()
                            }
                        }
                    }
                    mDisplayCount++
                    LogWriter.i("Image display time：${(System.currentTimeMillis() - startTime)/1000.0}s,display count:$mDisplayCount,run time:${MyAppUtils.formatHms(System.currentTimeMillis() - startRunTime)}")

                    callSuccess()
                } catch (e: Exception) {
                    callFail(e)
                } finally {
                    isBusy = false
                }
            }
        })
    }
    var startRunTime = 0L
    var mDisplayCount =  0

    override fun initScreen(startX: Int, startY: Int, width: Int, height: Int) {

        LogWriter.i("Detected that the device was last powered off,perform startup initialization")
        val ret = t1000Device!!.clearDisplay(startX, startY, width, height, MODE_INIT)
        if (ret != ErrorCode.ERROR_OK) {
            throw java.lang.Exception("Startup initialization failed：$ret")
        }
        LogWriter.i("Startup initialization successful")
    }

    override fun runConsumer() {

        LogWriter.i("====Start consumption thread")
        val instance = instance()
        startRunTime = System.currentTimeMillis()
        mDisplayCount = 0
        while (isRunning) {
            checkUsbDetachedOnPowerOff()

            if (mExceptionStop) {
                val ret = instance.powerT1000(false)
                LogWriter.i("Communication abnormality, T1000 power off,ret:$ret")
                SystemClock.sleep(SLEEP_TIME_NORMAL.toLong())
                mExceptionStop = false
                continue
            }
            if (isBusy) {
                SystemClock.sleep(SLEEP_TIME_BUSY.toLong())
                continue
            }
            //Device detection and power on/off
            if (instance.isEnableMcu) {
                val existDevice: Boolean = isExistDevice
                if (!existDevice) {
                    val b = instance.initMcu()
                    if (!b) {
                        SystemClock.sleep(SLEEP_TIME_EXCEPTION.toLong())
                        continue
                    }
                    var device: UsbDevice?
                    var retryCount = 0
                    do {
                        device = pollGetDevice()
                        if (!isRunning) {
                            hasDevice = false
                            break
                        }
                        if (device == null) {
                            retryCount++
                            val ret = instance.powerT1000(false)
                            devicePowerOnExceptionCount++
                            LogWriter.i("$devicePowerOnExceptionCount:No device found during power on, T1000 powered off,ret:$ret")
                            hasDevice = false
                            SystemClock.sleep(SLEEP_TIME_NORMAL.toLong())
                        }
                    } while (device == null && retryCount < 2)
                    if (device != null) {
                        initNewConnectInstance(device)
                    } else {
                        LogWriter.w("No device found after multiple power ups")
                        if (!isRunning) {
                            break
                        }
                        SystemClock.sleep(SLEEP_TIME_EXCEPTION.toLong())
                        continue
                    }
                } else {
                    if (!hasDevice) {
                        val device: UsbDevice? = AbstractT1000Helper.device
                        if (device == null) {
                            LogWriter.e("Device not found")
                            SystemClock.sleep(SLEEP_TIME_EXCEPTION.toLong())
                            continue
                        }
                        initNewConnectInstance(device)
                    }
                }
            }
            //Consumer pictures
            val poll = image
            if (poll == null) {
                //This waiting time can be interrupted
//                LogUtils.w("Waiting for production images ...")
                sleepWithInterrupted(SLEEP_TIME_NORMAL)
                continue
            }
            val intervalTime = poll.intervalTime
            if (poll.formatType == EpdImage.FORMAT_TYPE_FILE) {
                val epdImages = epdImageWrap.epdImages
                val currentPlayIndex = epdImages!!.indexOf(poll)
                LogWriter.i("======Trigger image transmission,currentPlayIndex=" + currentPlayIndex + ",intervalTime:" + intervalTime + ",filename:" + poll.name)
                //Update current index
                this@T1000Helper.currentPlayIndex = currentPlayIndex
                currentIntervalTime = intervalTime
            }
            instance().isT1000Power = true
            //Execute image display
            val performStatus = perform(poll)
            if (performStatus != ErrorCode.ERROR_OK) {

                //Interrupted abnormal insomnia
                if (performStatus == DisplayErrorCode.ERROR_INTERRUPT) {
                    continue
                }

                try {
                    isSleep = true
                    Thread.sleep(SLEEP_TIME_EXCEPTION.toLong())
                } catch (e: InterruptedException) {
                } finally {
                    isSleep = false
                }
            }
        }
        LogWriter.w("===The image display that the consumption thread has stopped,mExceptionStop:$mExceptionStop,mSleep:$isSleep===")
    }

    /**
     * Execute image display
     *
     * @param poll
     * @return
     */
    private fun perform(poll: EpdImage): Int {
        val exceptionStatus = IntArray(1)
        display(poll, object : T1000DisplayListener {
            override fun onSuccess() {
                imageDisplayChangeListener?.onSuccess(poll)
                exceptionStatus[0] = ErrorCode.ERROR_OK
                notifyDisplay()
            }

            override fun onFail(e: Exception) {
                LogWriter.e("Abnormal image transmission", e)
                imageDisplayChangeListener?.onFail(e)
                if (e is LoadException
                    || e is DisplayException || e is DeviceConnectException) {
                    LogWriter.e("Detected abnormal USB communication on the device", e)
                    mExceptionStop = true
                    exceptionStatus[0] = ErrorCode.ERROR_FAIL
                } else if (e is DisplayInterruptException) {
                    exceptionStatus[0] = DisplayErrorCode.ERROR_INTERRUPT
                } else {
                    exceptionStatus[0] = ErrorCode.ERROR_CHECK
                }
                notifyDisplay()
            }
        })

        //Waiting for the results to be displayed above
        try {
            waitDisplay()
        } catch (e: InterruptedException) {
            //ignore
        }

        if (exceptionStatus[0] != ErrorCode.ERROR_OK) {
            return exceptionStatus[0]
        } else {
            if (forceIgnoreSleep) {
                LogWriter.w("===New task discovered, force to ignore this hibernation,epdImage:" + poll.name)
                //Reset Tag
                forceIgnoreSleep = false
                return ErrorCode.ERROR_OK
            }
            try {
                val intervalTime = poll.intervalTime
                val enableMcu = instance().isEnableMcu
                if (enableMcu) {

                    //Low power processing，implemented by users
//                McuHelper.instance().startLowPower(intervalTime);

                    isSleep = true
                    //Display successful, sleep waiting until the next image is displayed
                    try {
                        LogWriter.i("This sleep time：${intervalTime}s")
                        Thread.sleep((intervalTime * 1000).toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                        //ignore
                    }
                }
            } finally {
                isSleep = false
            }
        }
        return ErrorCode.ERROR_OK
    }

    private fun pollGetDevice(): UsbDevice? {
        if (!isRunning) {
            return null
        }
        val ret = instance().powerT1000(true)
        if (ret == ErrorCode.ERROR_OK) {
            LogWriter.i("====T1000 successfully powered on.")
            PollUtils.pollTimeWithJump((10 * 1000).toLong(), object : PollUtils.MyCallable<Boolean> {
                override fun call(): Boolean {
                    if (!isRunning) {
                        return true
                    }
                    val effectiveDevice: Boolean = isExistDevice
                    if (!effectiveDevice) {
                        SystemClock.sleep(200)
                    }
                    return effectiveDevice
                }
            })
            return device
        } else {
            LogWriter.e("T1000 failed to power on，ret：$ret")
        }
        return null
    }

    private fun initNewConnectInstance(device: UsbDevice) {
        hasDevice = true
        devicePowerOnExceptionCount = 0
        t1000Device?.close()
        //Initialize a new connection instance
        t1000Device = T1000Device(device)
        screenType?.let {
            t1000Device?.setType(it.type)
        }

    }

    @Throws(InterruptedException::class)
    private fun waitDisplay() {
        synchronized(obj) {
            try {
                obj.wait()
            } catch (e: InterruptedException) {
                throw e
            }
        }
    }

    private fun notifyDisplay() {
        synchronized(obj) { obj.notifyAll() }
    }


    override fun upgradeFirmware(firmwareFile: File) {
        val b = upgradeCheck()
        if (!b) {
            LogWriter.e("Upgrade firmware verification failed")
            return
        }
        LogWriter.i("Start upgrading T1000 firmware, please wait... " + firmwareFile.name)
        val ret = t1000Device!!.upgradeFirmware(firmwareFile)
        if (ret == ErrorCode.ERROR_OK) {
            LogWriter.i("===T1000 firmware upgrade successful===")
            rebootOnUpgrade()
        } else {
            LogWriter.e("===T1000 firmware upgrade failed===,ret:$ret")
        }
    }


    override fun upgradeWbf(wbfFile: File) {
        val b = upgradeCheck()
        if (!b) {
            LogWriter.e("Upgrade WBF verification failed")
            return
        }
        LogWriter.i("Start upgrading T1000 waveform, please wait... " + wbfFile.name)
        val ret = t1000Device!!.upgradeWbf(wbfFile)
        if (ret == ErrorCode.ERROR_OK) {
            LogWriter.i("===T1000 waveform upgrade successful===")
            rebootOnUpgrade()
        } else {
            LogWriter.e("===T1000 waveform upgrade failed===,ret:$ret")
        }
    }

    /**
     * Upgrade verification
     *
     * @return
     */
    private fun upgradeCheck(): Boolean {
        var device: UsbDevice? = device
        if (device == null) {
            LogWriter.e("Upgrade verification, device not found, attempt to power on")
            val instance = instance()
            if (!instance.isConnected) {
                val ret = instance.open()
                if (ret != ErrorCode.ERROR_OK) {
                    LogWriter.e("===MCU connection failed,ret:$ret")
                    return false
                } else {
                    LogWriter.i("===MCU connection successful")
                }
            }
            SystemClock.sleep(500)
            val ret = instance.powerT1000(true)
            if (ret != ErrorCode.ERROR_OK) {
                LogWriter.e("===T1000 failed to power on,ret:$ret")
                return false
            }
            LogWriter.i("===T1000 successfully powered on")
            PollUtils.pollTimeWithJump((12 * 1000).toLong(), object : PollUtils.MyCallable<Boolean> {
                override fun call(): Boolean {
                    val effectiveDevice: Boolean = Companion.isExistDevice
                    if (effectiveDevice) {
                        SystemClock.sleep(200)
                    }
                    return effectiveDevice
                }
            })
            device = AbstractT1000Helper.device
            if (device == null) {
                return false
            }
        }
        requestPermission(device, object : UsbPermissionListener {
            override fun onGranted(usbDevice: UsbDevice) {
                LogWriter.i("USB authorization successful")
            }

            override fun onDenied(usbDevice: UsbDevice) {}
        })
        initNewConnectInstance(device)
        val ret = open()
        if (ret != ErrorCode.ERROR_OK) {
            LogWriter.e("connection failed，ret：$ret")
            return false
        }
        return true
    }

    /**
     * Restart after upgrading
     */
    private fun rebootOnUpgrade() {
        SystemClock.sleep(2000)
        val instance = instance()
        if (!instance.isConnected) {
            val open = instance.open()
            if (open != ErrorCode.ERROR_OK) {
                LogWriter.e("===MCU connection failed,ret:$open")
                return
            }
            SystemClock.sleep(500)
        }
        var reboot: Boolean
        var attemptCount = 0
        do {
            reboot = instance.reboot()
            attemptCount++
        } while (!reboot && attemptCount < 3)
        LogWriter.w("Firmware upgrade and restart device：$reboot")
    }
}
