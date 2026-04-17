package com.xingtai.epd.device.demo.util

import com.sjl.util.FileUtils
import com.sjl.util.LogWriter
import com.xingtai.device.t1000.entity.ScreenType
import com.xingtai.epd.device.demo.app.AppConstant
import com.xingtai.epd.device.demo.app.AppConstant.getAppConfigDir
import com.xingtai.epd.device.demo.entity.AppConfig
import com.xingtai.epd.device.demo.entity.BatteryType
import java.io.File
import java.io.IOException

/**
 * App configuration tool class
 *
 * @author Kelly
 * @version 1.0.0
 * @filename AppConfigUtils
 * @time 2023/9/13 9:52
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
object AppConfigUtils {
    /**
     * Read the configuration and assign the global 'AppConfig' value
     * @return
     */
    fun readConfig(): AppConfig {
        val appConfigFile = File(getAppConfigDir(), AppConstant.APP_CONFIG_NAME)
        var appConfig: AppConfig? = null
        if (appConfigFile.exists()) {
            try {
                val s = FileUtils.readFileToString(appConfigFile)
                appConfig = GsonUtils.fromJson(s, AppConfig::class.java)
            } catch (e: IOException) {
                LogWriter.e("Abnormal reading of 'AppConfig'", e)
            }
        }
        if (appConfig == null) {
            appConfig = AppConfig()
        }
        AppConstant.appConfig = appConfig
        LogWriter.i("====== appConfig:" + AppConstant.appConfig)
        return appConfig
    }

    /**
     * Write configuration and update the global 'AppConfig' value
     *
     * @param appConfig
     * @return
     */
    fun writeConfig(appConfig: AppConfig): Boolean {
        val s = GsonUtils.toJson(appConfig)
        val appConfigFile = File(getAppConfigDir(), AppConstant.APP_CONFIG_NAME)
        try {
            FileUtils.writeTextToFile(appConfigFile, s)
            AppConstant.appConfig = appConfig
            return true
        } catch (e: Exception) {
            LogWriter.e("Writing 'AppConfig' Exception", e)
        }
        return false
    }

    val appConfig: AppConfig
        /**
         * Get app configuration information
         * @return
         */
        get() {
            val appConfig = AppConstant.appConfig
            return appConfig ?: readConfig()
        }


    val screenType: ScreenType
        /**
         * Get screen type
         */
        get() {
            val appConfig = appConfig
            val type = appConfig.type
            return ScreenType.parseByType(type)
        }


    /**
     * Get battery type
     *
     */
    fun getBatteryType(): BatteryType? {
        val appConfig: AppConfig = appConfig
        return BatteryType.parseByType(appConfig.batteryType)
    }


    /**
     * Get autoOpenLight flag
     * @return
     */
    fun isAutoOpenLight(): Boolean {
        val appConfig: AppConfig = appConfig
        return appConfig.autoOpenLight
    }


    /**
     *
     * Update the startup auto-launch indicator
     *
     * @param enable
     */
    fun updateAutoBootApp(enable: Boolean) {
        val appConfig: AppConfig = appConfig
        appConfig.autoBootApp = enable
        writeConfig(appConfig)
    }

    /**
     * Enable startup auto-launch?
     * @return
     */
    fun isAutoBootApp(): Boolean {
        val appConfig: AppConfig = appConfig
        return appConfig.autoBootApp
    }
}
