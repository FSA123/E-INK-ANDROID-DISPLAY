package com.xingtai.epd.device.demo.mcu

import android.os.SystemClock
import com.sjl.deviceconnector.ErrorCode
import com.sjl.util.LogWriter
import com.xingtai.device.mcu.Mcu
import com.xingtai.device.mcu.McuApiImpl
import com.xingtai.device.mcu.entity.McuInfo
import com.xingtai.device.mcu.listener.BatteryChangeListener
import com.xingtai.device.mcu.listener.McuKeyListener
import com.xingtai.device.t1000.entity.ScreenType
import com.xingtai.epd.device.demo.app.MyApplication
import com.xingtai.epd.device.demo.entity.BatteryType
import com.xingtai.epd.device.demo.sys.SysApiFactory
import com.xingtai.epd.device.demo.util.AppConfigUtils
import com.xingtai.epd.device.demo.util.MyAppUtils
import com.xingtai.epd.device.demo.util.SpUtils.getSerialConfig
import com.xingtai.sys.api.smt40a.Smt40aApi
import java.io.File
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger

/**
 * mcu help class
 *
 * @author Kelly
 * @version 1.0.0
 * @filename McuHelper
 * @time 2023/5/18 9:46
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
class McuHelper private constructor() {
    private var mcuApi: McuApiImpl? = null
    private var timer: Timer? = null

    /**
     * True if the MCU on the power board does not receive a heartbeat from the Android board for three minutes (such as an Android program crashing),it will power off the Android board.
     * False no further action will be taken until the battery is depleted
     */
    var mcuHeartbeatEnable = true
    /**
     * T1000 power switch flag
     *
     * @return
     */
    var isT1000Power = false
    var lastT1000PowerOff = false

    /**
     * front light power switch flag
     *
     * @return
     */
    var isFrontLightPower = false


    /**
     * heartbeat flag
     *
     * @return
     */
    var isNormalHeartbeat = false

    /**
     * Can be added to the configuration items
     */
    fun getBatteryType(): BatteryType? {
        var batteryType: BatteryType? = AppConfigUtils.getBatteryType()
        if (batteryType != null) {
            return batteryType
        }
        return BatteryType.BATTERY_4S1P
    }

    var oldSoc = 0

    init {
        initMcuApi()
    }

    private object InstanceHolder {
        var helper = McuHelper()
    }

    val isEnableMcu: Boolean = true


    /**
     * Initialize MCU
     *
     * @return
     */
    @Synchronized
    fun initMcu(): Boolean {
        var ret = state
        if (ret != ErrorCode.ERROR_OK) {
            ret = open()
            return if (ret != ErrorCode.ERROR_OK) {
                LogWriter.i("MCU serial port opening failed：$ret")
                false
            } else {
                SystemClock.sleep(100)

                //Turn on low power consumption(optional)
                if (versionCode >= McuApiImpl.VERSION_CODE_108) {
                    startLowPower(mcuHeartbeatEnable)
                }
                if (mcuHeartbeatEnable){
                    stopHeartbeatTimer()
                    // Start heartbeat(optional)
                    startHeartbeatTimer()
                }

                LogWriter.i("Initialize MCU successfully")
                //TODO:Here, you can set the startup to automatically turn on the front light
                if (AppConfigUtils.isAutoOpenLight()) {
                    MyApplication.getMainThreadExecutor().postDelayed(Runnable {
                        powerFrontLight(true)
                    }, 3000)
                }

                true
            }
        }
        return true
    }

    fun initMcuApi() {
        if (mcuApi == null) {
            mcuApi = McuApiImpl(mcuUartPath)
        }
    }

    fun getMcuApi(): McuApiImpl? {
        initMcuApi()
        return mcuApi
    }

    fun open(): Int {
        return getMcuApi()!!.open()
    }

    val state: Int
        get() = getMcuApi()!!.state
    val isConnected: Boolean
        get() = state == ErrorCode.ERROR_OK

    @Synchronized
    fun close() {
        isNormalHeartbeat = false
        isT1000Power = false
        isFrontLightPower = false
        stopHeartbeatTimer()

        if (isEnableMcu) {
            //Turn off the front light
            if (isFrontLightPower) {
                powerFrontLight(false)
            }
            //Turn off low power consumption
            if (mcuHeartbeatEnable && versionCode >= McuApiImpl.VERSION_CODE_108) {
                startLowPower(false)
            }
            val state = getMcuApi()!!.state
            LogWriter.i("state:$state")
            getMcuApi()?.close()
        }
        mcuApi = null
        LogWriter.w("mcu close")
    }

    /**
     * Low power on and off
     *
     * @param enable true: turn on，false: turn off
     * @return
     * @since 1.0.8 support
     */
    fun startLowPower(enable: Boolean): Boolean {
        val ret = getMcuApi()!!.startLowPower(enable)
        LogWriter.i("Low power consumption:" + (if (enable) "open" else "close") + ",ret：" + ret)
        return ret == ErrorCode.ERROR_OK
    }

    val mcuInfo: McuInfo?
        /**
         * Obtain MCU control board information
         *
         * @return
         * @since 1.2.0 support
         */
        @Synchronized
        get() {
            if (!isConnected) {
                return null
            }
            val mcuInfo = McuInfo()
            val state: Int
            try {
                state = getMcuApi()!!.getMcuInfo(mcuInfo)
                LogWriter.i("mcuInfo state:$state,mcuInfo:$mcuInfo,Remaining battery capacity：" + getCompatSoc(
                    mcuInfo.charge,
                    mcuInfo.voltage,
                    mcuInfo.soc) + "%")
                if (state == ErrorCode.ERROR_OK) {
                    return mcuInfo
                }
            } catch (e: Exception) {
                LogWriter.e("Abnormal acquisition of MCU control board information:" + e.message)
            }
            return null
        }

    /**
     * Battery percentage
     *
     * @param voltage Unit: mV
     * @return
     */
    fun batteryVoltageToSoc(voltage: Int): Int {
        var batteryType = getBatteryType()
        val minVoltageThreshold: Float
        val maxVoltageThreshold: Float
        if (batteryType == BatteryType.BATTERY_4S1P) {
            minVoltageThreshold = batteryType.fullyDischargeVoltage + 2.7f
            maxVoltageThreshold = batteryType.fullyChargeVoltage
        } else {
            batteryType = BatteryType.BATTERY_3S1P
            minVoltageThreshold = batteryType.fullyDischargeVoltage + 1.5f
            maxVoltageThreshold = batteryType.fullyChargeVoltage - 0.08f
        }
        var soc =
            ((voltage / 1000.0 - minVoltageThreshold) / (maxVoltageThreshold - minVoltageThreshold) * 100).toInt()
        if (soc <= 0) {
            soc = 0
        }
        if (soc >= 100) {
            soc = 100
        }
        return soc
    }

    /**
     * Voltage SOC
     *
     * @param charge
     * @param mV
     * @param batterySoc
     * @return
     */
    fun getCompatSoc(charge: Int, mV: Int, batterySoc: Int): Int {
        var soc: Int
        if (versionCode > McuApiImpl.VERSION_CODE_117) {
            //Power board firmware version greater than 1.1.7, supports returning battery percentage
            soc = batterySoc
            oldSoc = soc
        } else {
            soc = batteryVoltageToSoc(mV)
            if (charge == Mcu.BATTERY_UNCHARGED && oldSoc > 0) {
                //Only decrease, not increase
                if (soc <= oldSoc) {
                    oldSoc = soc
                } else {
                    soc = oldSoc
                }
            } else if (charge == Mcu.BATTERY_CHARGING) {
                //Only increase, not decrease
                if (soc >= oldSoc) {
                    oldSoc = soc
                } else {
                    soc = oldSoc
                }
            } else {
                oldSoc = soc
            }
        }
        return soc
    }

    @Synchronized
    fun sendHeartbeat(): Int {
        val ret = getMcuApi()!!.sendHeartbeat()
        isNormalHeartbeat = ret == ErrorCode.ERROR_OK
        return ret
    }

    fun startHeartbeatTimer() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                val ret = sendHeartbeat()
                if (ret != ErrorCode.ERROR_OK) {
                    LogWriter.w("MCU heartbeat failure：$ret")
                } else {
                    LogWriter.i("MCU heartbeat successful")
                }
            }
        }, 5000, HEARTBEAT_TIMEOUT.toLong())
    }

    fun stopHeartbeatTimer() {
        timer?.cancel()
    }

    @Synchronized
    private fun powerControl(mainPower: Boolean,
                             delayTime: Int,
                             t1000Power: Boolean?,
                             fivePower: Boolean?): Int {
        return getMcuApi()!!.powerControl(mainPower, delayTime, t1000Power, fivePower)
    }

    /**
     * Front brightness adjustment
     *
     * @param flag
     */
    fun adjustBrightness(flag: Boolean): Boolean {
        return if (versionCode > McuApiImpl.VERSION_CODE_119) {
            if (!isFrontLightPower) {
                powerFrontLight(true)
            }
            mcuInfo ?: return false
            var brightnessLevel = getMcuApi()!!.brightnessLevel
            val maxBrightnessLevel =
                maxBrightnessLevel
            if (flag) {
                if (brightnessLevel >= maxBrightnessLevel) {
                    brightnessLevel = maxBrightnessLevel
                } else {
                    brightnessLevel += 1
                }
            } else {
                if (brightnessLevel <= 1) {
                    brightnessLevel = 1
                } else {
                    brightnessLevel -= 1
                }
            }
            val ret = getMcuApi()!!.adjustBrightness(brightnessLevel)
            LogWriter.i("brightnessLevel:$brightnessLevel,ret:$ret")
            ret == ErrorCode.ERROR_OK
        } else {
            //unsupported
            false
        }
    }

    /**
     * Front brightness adjustment
     *
     * @param brightnessLevel
     * @return
     */
    fun adjustBrightness(brightnessLevel: Int): Boolean {
        return if (versionCode > McuApiImpl.VERSION_CODE_119) {
            val ret = getMcuApi()!!.adjustBrightness(brightnessLevel)
            LogWriter.i("brightnessLevel direct:$brightnessLevel,ret:$ret")
            ret == ErrorCode.ERROR_OK
        } else {
            false
        }
    }

    /**
     * Delayed boot
     *
     * @param delayTime second
     * @return
     */
    fun boot(delayTime: Int): Int {
        val no = atomicNum.incrementAndGet()
        return getMcuApi()!!.boot(no, delayTime)
    }

    /**
     * shutdown
     *
     * @param time second
     * @return
     */
    fun shutdown(time: Int): Boolean {
        val ret = powerMain(false, time)
        LogWriter.i("Execute the shutdown of the main power supply，ret:$ret")
        return ret == ErrorCode.ERROR_OK
    }

    /**
     * reboot after 3 seconds
     */
    fun reboot(): Boolean {
        //设置延时开机
        val time = 3
        val ret = boot(time)
        return if (ret == ErrorCode.ERROR_OK) {
            SystemClock.sleep(200)
            shutdown(1)
            true
        } else {
            LogWriter.w("Setting reboot failed，ret：$ret")
            false
        }
    }

    /**
     * T1000 power on/off
     *
     * @param t1000Power
     * @return
     */
    fun powerT1000(t1000Power: Boolean): Int {
        if (!isEnableMcu) {
            return ErrorCode.ERROR_NOT_SUPPORTED
        }
        val screenType = AppConfigUtils.screenType
        if (is133Screen(screenType)) {
            //IT 8951 is powered by USB
            val sysApi = SysApiFactory.sysApi
            return if (sysApi is Smt40aApi) {
                //Delayed power outage
                if (!t1000Power) {
                    SystemClock.sleep(1000)
                }
                val ret = sysApi.setUsbPower(2, if (t1000Power) 1 else 0)
                LogWriter.i("$t1000Power: usb power control,ret：$ret")
                if (!t1000Power) {
                    SystemClock.sleep(200)
                }
                if (ret == ErrorCode.ERROR_OK) {
                    isT1000Power = t1000Power
                    lastT1000PowerOff = true
                }
                ret
            } else {
                ErrorCode.ERROR_NOT_SUPPORTED
            }
        } else if (ScreenType.isMulti8951Screen(screenType)) {
            //Delayed power outage
            if (!t1000Power) {
                SystemClock.sleep(1000)
            }
        } else {
            SystemClock.sleep(500)
        }
        val ret: Int = if (versionCode > McuApiImpl.VERSION_CODE) {
            //Only higher versions support independent control of power supply
            powerControl(true, 5, t1000Power, null)
        } else {
            powerControl(true, 5, t1000Power, isFrontLightPower)
        }
        if (ret == ErrorCode.ERROR_OK) {
            isT1000Power = t1000Power
            lastT1000PowerOff = true
        }
        return ret
    }

    /**
     * Front light control
     *
     * @param frontLightPower
     * @return
     */
    fun powerFrontLight(frontLightPower: Boolean): Boolean {
        val ret: Int = if (versionCode > McuApiImpl.VERSION_CODE) {
            //Only higher versions support independent control of power supply
            powerControl(true, 5, null, frontLightPower)
        } else {
            powerControl(true, 5, isT1000Power, frontLightPower)
        }
        LogWriter.i("Front light control power:$frontLightPower,ret:$ret")
        return if (ret == ErrorCode.ERROR_OK) {
            isFrontLightPower = frontLightPower
            true
        } else {
            false
        }
    }

    /**
     * Power on and off the main power supply
     *
     * @param mainPower
     * @param delayTime
     * @return
     */
    fun powerMain(mainPower: Boolean, delayTime: Int): Int {
        return powerControl(mainPower, delayTime, false, false)
    }

    fun getKeyDelay(keyDelayFlag: Int): Int? {
        return getMcuApi()!!.keyDelay[keyDelayFlag]
    }

    val versionCode: Int
        get() = getMcuApi()!!.versionCode

    /**
     * Enter different power consumption modes according to different interval time
     *
     * - The display strategy is customized by the user，such as,
     *  1. Interval time is  0-30 seconds, T1000 power on
     *  2. Interval time is  30-300 seconds,T1000 power off
     *  3. Interval time exceeds 300 seconds,main power off
     *
     * @param intervalTime
     */
    @JvmOverloads
    fun startLowPower(intervalTime: Float, allowSleep: Boolean = true) {
        if (!isEnableMcu) {
            return
        }
        if (intervalTime <= INTERVAL_TIME_OPEN) {
            LogWriter.i("$intervalTime===T1000 power on mode===")
        } else if (intervalTime > INTERVAL_TIME_OPEN && intervalTime <= INTERVAL_TIME_CLOSE) {
            LogWriter.w("$intervalTime===T1000 power off mode===")
            val ret = powerT1000(false)
            if (ret != ErrorCode.ERROR_OK) {
                LogWriter.w("T1000 power failure，ret:$ret")
            } else {
                LogWriter.i("T1000 power off successfully.")
            }
        } else {
            LogWriter.w("$intervalTime=== Main power off mode==")

            var ret = powerT1000(false)
            if (ret != ErrorCode.ERROR_OK) {
                LogWriter.w("T1000 power off successfully，ret:$ret")
            } else {
                LogWriter.i("T1000 power off successfully.")
            }

            //Sleep and set delayed startup,deep sleep saves the most electricity
            ret = boot(intervalTime.toInt())
            if (ret == ErrorCode.ERROR_OK) {
                val time = 3
                SystemClock.sleep(1000)
                //Delay by 3 seconds to turn off the main power supply
                shutdown(time)
                //This delay is to prevent the consumption of images from continuing before shutting down
                SystemClock.sleep(((time + 1) * 1000).toLong())
            } else {
                LogWriter.w("Setting delayed startup failed，ret：$ret")
            }
        }
    }

    /**
     * Upgrade firmware, it will shut down after upgrading
     *
     * @param firmwareFile
     */
    fun upgradeFirmware(firmwareFile: File) {
        val b = upgradeCheck()
        if (!b) {
            return
        }
        SystemClock.sleep(500)
        LogWriter.i("Start upgrading MCU firmware, please wait... " + firmwareFile.name)
        var ret = 0
        var attemptCount = 0
        do {
            if (attemptCount >= 1) {
                LogWriter.w("ret:$ret,MCU firmware upgrade retry,connectCount：$attemptCount")
            }
            ret = getMcuApi()!!.upgradeFirmware(firmwareFile)
            attemptCount++
        } while (ret != ErrorCode.ERROR_OK && attemptCount < 5)
        if (ret == ErrorCode.ERROR_OK) {
            MyAppUtils.flushCache()
            LogWriter.i("MCU firmware upgrade successful")
        } else {
            LogWriter.e("MCU firmware upgrade failed：$ret")
        }
    }

    /**
     * Upgrade verification (battery level)
     *
     * @return
     */
    private fun upgradeCheck(): Boolean {
        if (versionCode < McuApiImpl.VERSION_CODE_111) { //1.1.0 and above support OTA
            LogWriter.w("The current version does not support OTA，versionCode：$versionCode")
            return false
        }
        val ret = open()
        if (ret != ErrorCode.ERROR_OK) {
            LogWriter.e("MCU connection failed，ret：$ret")
            return false
        }
        LogWriter.i("MCU successfully opened")
        return true
    }

    /**
     * Key listening
     *
     * @param mcuKeyListener
     */
    fun setMcuKeyListener(mcuKeyListener: McuKeyListener?) {
        getMcuApi()!!.setMcuKeyListener(mcuKeyListener)
    }

    /**
     * Battery level change listening
     *
     * @param batteryChangeListener
     */
    fun setBatteryChangeListener(batteryChangeListener: BatteryChangeListener?) {
        getMcuApi()!!.setBatteryChangeListener(batteryChangeListener)
    }

    companion object {
        private const val HEARTBEAT_TIMEOUT = 60 * 1000
        private val atomicNum = AtomicInteger()
        private const val INTERVAL_TIME_OPEN = 30
        const val INTERVAL_TIME_CLOSE = 300
        @JvmStatic
        fun instance(): McuHelper {
            return InstanceHolder.helper
        }

        private val mcuUartPath: String?
            /**
             * Get serial port path
             *
             * @return
             */
            private get() {
                val serialConfig = getSerialConfig()
                return serialConfig?.devicePath
            }
        val maxBrightnessLevel: Int
            /**
             * Maximum brightness level
             *
             * @return
             */
            get() {
                val screenType = AppConfigUtils.screenType
                var maxBrightnessLevel = 10
                if (is133Screen(screenType)) {
                    maxBrightnessLevel = 5
                }
                return maxBrightnessLevel
            }

        private fun is133Screen(screenType: ScreenType?): Boolean {
            return screenType == ScreenType.SCREEN_13_3_MONOCHROME || screenType == ScreenType.SCREEN_13_3_KALEIDOSCOPE
        }
    }
}
