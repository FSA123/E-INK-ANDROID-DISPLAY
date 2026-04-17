package com.xingtai.epd.device.demo.t1000

import android.hardware.usb.UsbDevice
import android.os.SystemClock
import com.sjl.deviceconnector.ErrorCode
import com.sjl.deviceconnector.device.usb.UsbHelper
import com.sjl.deviceconnector.listener.UsbPermissionListener
import com.sjl.util.LogUtils
import com.sjl.util.LogWriter
import com.xingtai.device.t1000.entity.ScreenType
import com.xingtai.device.t1000.entity.T1000DeviceInfo
import com.xingtai.device.t1000.entity.T1000UsbDevice
import com.xingtai.epd.device.demo.mcu.McuHelper.Companion.instance
import com.xingtai.epd.device.demo.t1000.entity.EpdImage
import com.xingtai.epd.device.demo.t1000.listener.ImageDisplayChangeListener
import com.xingtai.epd.device.demo.t1000.listener.T1000DisplayListener
import com.xingtai.epd.device.demo.t1000.util.EpdImageWrap
import com.xingtai.epd.device.demo.util.PollUtils
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Abstract base class, universal method
 *
 * @author Kelly
 * @version 1.0.0
 * @filename AbstractT1000Helper
 * @time 2023/11/30 11:51
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
abstract class AbstractT1000Helper : IT1000Helper {
    /**
     * Queue (Level 1 Cache)
     */
    private var mQueue: BlockingQueue<EpdImage> = LinkedBlockingQueue(MAX_IMAGES)
    protected var hasDevice = true



    /**
     * Abnormal power on counter of equipment
     */
    protected var devicePowerOnExceptionCount = 0

    /**
     * Current screen type
     */
    var screenType: ScreenType? = null

    var epdImageWrap = EpdImageWrap()

    /**
     * This index is updated only during the execution of image brushing
     *
     * @return
     */
    /**
     * 当前播放的索引
     */
    var currentPlayIndex = -1
    protected var currentIntervalTime = -1f

    /**
     * Mark whether the image display is being processed
     */
    @Volatile
    var isBusy = false

    /**
     * After processing the image, is it in sleep mode waiting for the next round of playback
     */
    @Volatile
    var isSleep = false

    /**
     * Forcefully ignore sleep
     */
    var forceIgnoreSleep = false

    protected var t1000DeviceInfo: T1000DeviceInfo? = null
    protected var t1000DisplayListener: T1000DisplayListener? = null
    var imageDisplayChangeListener: ImageDisplayChangeListener? = null

    /**
     * Consumer thread
     */
    private var mConsumerThread: Thread? = null
    private var mConsumerRunnable: Runnable? = null
    private val isStarting = AtomicBoolean(false)

    @Volatile
    var isRunning = false
        protected set

    /**
     * Used for self checking and self repairing device discovery
     */
    @Volatile
    protected var mExceptionStop = false

    /**
     * Is the USB device unplugged
     */
    @Volatile
    var isUsbDetached = false

    /**
     * Start consumer thread
     */
    private fun startConsumerThread() {

        if (!isStarting.compareAndSet(false, true)) {
            LogWriter.w("The consumption thread is running and cannot be restarted repeatedly...")
            return
        }
        if (isRunning) {
            LogWriter.w("Consumer thread is running...")
            return
        }
        if (mConsumerThread?.isAlive == true) {
            LogWriter.w("Consumer thread is alive...")
            return
        }
        isRunning = true
        mExceptionStop = false

        mConsumerRunnable = Runnable {
            try {
                runConsumer()
            } finally {
                isStarting.set(false)
            }
        }
        mConsumerThread = Thread(mConsumerRunnable)
        mConsumerThread?.start()
    }

    /**
     * Run consumer
     */
    protected abstract fun runConsumer()

    /**
     * Stop consumer thread
     */
    private fun stopConsumerThread() {
        isRunning = false
        mExceptionStop = false
        mConsumerThread?.run {
            if (isAlive){
                interrupt()
                mConsumerRunnable = null
                mConsumerThread = null
            }
        }

    }

    /**
     * Check the status of the consumption thread
     * @return
     */
    fun isConsumerRunning(): Boolean {
        return isStarting.get()
    }

    /**
     * Interruptable thread sleep
     *
     * @param sleepTime
     */
    protected fun sleepWithInterrupted(sleepTime: Int) {
        try {
            isSleep = true
            try {
                Thread.sleep(sleepTime.toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        } finally {
            isSleep = false
        }
    }
    /**
     * Activate the display and retrieve images from the queue for display
     */
    override fun startDisplay() {
        startConsumerThread()
    }

    override fun disconnect() {
        isUsbDetached = false
        t1000DeviceInfo = null
        currentPlayIndex = -1
        val timeout: Int
        val screenType = screenType
        timeout = if (screenType != null && ScreenType.isColorsScreen(screenType)) {
            35 * 1000
        } else {
            12 * 1000
        }
        val start = System.currentTimeMillis()

        stopConsumerThread()
        checkConsumerThread()
        do {
            LogUtils.i("Disconnecting")
        } while (isBusy && System.currentTimeMillis() - start <= timeout)
        if (isBusy) {
            LogUtils.w("timeout")
        }
        close()
        t1000DisplayListener = null
        epdImageWrap.clearEpdImage()
        clearImage()
        LogWriter.i("T1000 connection disconnected successfully, time-consuming：" + (System.currentTimeMillis() - start) / 1000.0 + "s")
    }

    /**
     * Check if the consumption thread has completely exited
     */
    private fun checkConsumerThread() {
        if (isRunning) {
            LogWriter.i("Detected that the consumption thread is running...")
            PollUtils.pollTimeWithJump(10 * 1000, object : PollUtils.MyCallable<Boolean> {
                override fun call(): Boolean {
                    val isRunning: Boolean = isRunning || isConsumerRunning()
                    if (isRunning) {
                        SystemClock.sleep(200)
                    }
                    return !isRunning
                }

            })
        }
        LogWriter.i("Consumption thread running flag：$isRunning")
    }

    protected fun checkUsbDetachedOnPowerOff() {
        if (!isRunning) {
            return
        }
        //The reason for the inspection is that when there is a power outage, it takes time for the device to pop up, and if it communicates immediately, it will be abnormal
        if (instance().isEnableMcu && !instance().isT1000Power && imageSize > 0) {
            val oldUsbDetached = isUsbDetached
            LogWriter.w("Detected that T1000 has been powered off, check if the device has been unplugged：$oldUsbDetached")
            if (!oldUsbDetached) {
                PollUtils.pollTimeWithJump((3 * 1000).toLong(), object : PollUtils.MyCallable<Boolean> {
                    override fun call(): Boolean {
                        return if (!isRunning) {
                            true
                        } else isUsbDetached
                    }
                })
                //The detection has been unplugged, sleep for a while and then turn on the power, otherwise the power will not be detected immediately
                if (isUsbDetached) {
                    SystemClock.sleep(SLEEP_TIME_BUSY.toLong())
                }
            }
        }
        //Reset
        isUsbDetached = false
    }

    /**
     * Determine if it is a T 1000 device
     *
     * @param usbDevice
     * @return
     */
    fun isEffectiveDevice(usbDevice: UsbDevice): Boolean {
        return mSupportedDevices.contains(T1000UsbDevice(usbDevice.vendorId, usbDevice.productId))
    }

    /**
     * T1000 unplugged monitoring
     */
    fun closeOnDetached(usbDevice: UsbDevice) {
        //Power off, internal connections will be processed to prevent duplicate processing
        val enableMcu = instance().isEnableMcu
        if (enableMcu && !instance().isT1000Power) {
            LogWriter.w("T1000 has been powered off")
            close()
            return
        }
        requestSleepInterrupted()
        if (!isBusy && state == ErrorCode.ERROR_OK) {
            close()
        }
    }

    fun requestPermission(usbDevice: UsbDevice, usbPermissionListener: UsbPermissionListener?) {
        val device = UsbHelper.getDevice(usbDevice.vendorId, usbDevice.productId)
        if (device != null) {
            UsbHelper.getInstance().requestPermission(usbDevice, usbPermissionListener)
            LogWriter.i("The T1000 device exists")
        } else {
            LogWriter.e("The T1000 device does not exist")
        }
    }



    @Synchronized
    fun remove(nextEpdImage: EpdImage?) {
        val remove = epdImageWrap.remove(nextEpdImage)
    }

    /**
     * Request to allow sleep interruption without affecting the consumption thread, enabling timely processing of the latest image resources
     */
    fun requestSleepInterrupted() {
        LogUtils.i("isSleep:$isSleep")
        if (isSleep) {
            mConsumerThread?.interrupt()
        }else{
            forceIgnoreSleep = true
        }
    }

    val nextEpdImage: EpdImage?
        /**
         * Get the next image
         *
         * @return
         */
        get() = epdImageWrap.nextEpdImage

    /**
     * Every successful callback, auto increment index
     */
    fun autoIncrementCurrentPlayIndex() {
        epdImageWrap.autoIncrementCurrentPlayIndex()
    }

    /**
     * Initialize play resources
     *
     * @param epdImages
     */
    fun initEpdImage(epdImages: List<EpdImage>) {
        currentPlayIndex = -1
        clearImage()
        epdImageWrap.resetEpdImage(epdImages)
    }

    /**
     * Reset the resources being played
     *
     * @param epdImages
     */
    fun resetEpdImage(epdImages: List<EpdImage>) {
        initEpdImage(epdImages)
    }

    /**
     * Reset existing resources
     */
    fun resetCurrentEpdImage() {
        val epdImageList: List<EpdImage> = ArrayList(epdImageWrap.epdImages)
        resetEpdImage(epdImageList)
    }

    /**
     * Update the index of the image resource list
     *
     * @param currentImgIndex
     */
    fun updateCurrentImgIndex(currentImgIndex: Int) {
        LogUtils.w("Next play index：$currentImgIndex")
        epdImageWrap.currentPlayIndex = currentImgIndex
    }

    fun epdImageSize(): Int {
        return epdImageWrap.size()
    }

    /**
     * Is there an executable image available
     *
     * @return
     */
    fun hasAvailableEpdImages(): Boolean {
        return epdImageSize() > 0 || imageSize > 0
    }

    /**
     * Add to queue
     *
     * @param epdImage
     */
    fun addImage(epdImage: EpdImage): Boolean {
        var offer = false
        try {
            offer = mQueue.offer(epdImage, 50, TimeUnit.MILLISECONDS)
            LogUtils.i("addImage offer:" + offer + ",name:" + epdImage.name)
        } catch (e: InterruptedException) {
        }
        return offer
    }

    protected val image: EpdImage?
        protected get() =//Return the first element and delete it from the queue
            mQueue.poll()

    val imageSize: Int
        get() = mQueue.size

    /**
     * Clear queue
     */
    fun clearImage() {
        val iterator = mQueue.iterator()
        while (iterator.hasNext()) {
            iterator.remove()
        }
    }

    val isFull: Boolean
        /**
         * Check if the queue is full
         *
         * @return
         */
        get() {
            val imageSize = imageSize
            return imageSize == MAX_IMAGES
        }

    fun isExistImage(nextEpdImage: EpdImage): Boolean {
        return mQueue.contains(nextEpdImage)
    }


    protected fun callSuccess() {
        t1000DisplayListener?.onSuccess()
    }

    protected fun callFail(e: Exception) {
        t1000DisplayListener?.onFail(e)
    }

    companion object {
        /**
         * List of supported devices
         */
        var mSupportedDevices: List<T1000UsbDevice> = ArrayList<T1000UsbDevice>(listOf(T1000UsbDevice(1165, 35159), T1000UsbDevice(1165, 35153)))

        /**
         * Maximum number of cached images in the queue
         */
        private const val MAX_IMAGES = 2
        const val SLEEP_TIME_NORMAL = 5 * 1000
        const val SLEEP_TIME_BUSY = 2 * 1000
        const val SLEEP_TIME_EXCEPTION = 10 * 1000
        val isExistDevice: Boolean
            /**
             * Is there a device present
             *
             * @return
             */
            get() = device != null

        val device: UsbDevice?
            /**
             * Obtain T1000 device
             *
             * @return
             */
            get() {
                for (t1000UsbDevice in mSupportedDevices) {
                    val device =
                        UsbHelper.getDevice(t1000UsbDevice.vendorId, t1000UsbDevice.productId)
                    if (device != null) {
                        return device
                    }
                }
                return null
            }

        /**
         * list T1000 devices
         *
         * @return
         */
        fun listDevices(): List<UsbDevice> {
            val deviceList = UsbHelper.getDeviceList()
            val tempDeviceList: MutableList<UsbDevice> = ArrayList()
            for (usbDevice in deviceList) {
                if (mSupportedDevices.contains(T1000UsbDevice(usbDevice.vendorId,
                        usbDevice.productId))) {
                    tempDeviceList.add(usbDevice)
                }
            }
            return tempDeviceList
        }
    }
}
