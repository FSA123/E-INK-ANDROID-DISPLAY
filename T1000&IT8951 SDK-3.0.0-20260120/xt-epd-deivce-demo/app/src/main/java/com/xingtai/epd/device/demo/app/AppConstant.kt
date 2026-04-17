package com.xingtai.epd.device.demo.app

import android.os.Environment
import com.xingtai.epd.device.demo.entity.AppConfig
import java.io.File

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename AppConstant.java
 * @time 2018/2/18 21:48
 * @copyright(C) 2018 song
 */
object AppConstant {
    @JvmField
    val ROOT_PATH = Environment.getExternalStorageDirectory().toString() + File.separator // SD path

    /**
     * Download Dir
     */
    var EXTERNAL_SHARE_DOWNLOADS = Environment.getExternalStorageDirectory().toString() + File.separator + Environment.DIRECTORY_DOWNLOADS
    var INTERNAL_CACHE_PATH = MyApplication.getContext().cacheDir.absolutePath + File.separator
    var INTERNAL_FILES_PATH = MyApplication.getContext().filesDir.absolutePath + File.separator

    @JvmField
    val MY_ROOT_DIR = MyApplication.getContext().packageName

    const val APP_CONFIG_NAME = "app_config.json"


    @JvmField
    val MY_DOWNLOADS_PATH = EXTERNAL_SHARE_DOWNLOADS + File.separator + MY_ROOT_DIR

    @JvmField
    val APP_CONFIG_PATH = INTERNAL_FILES_PATH + File.separator + "config"


    @JvmField
    var appConfig: AppConfig? = null


    /**
     * External USB resource directory
     */
    @JvmField
    val MY_RESOURCES_USB= "My_Resources"

    /**
     * Adjust the frequency of image production
     */
    const val IMAGE_PRODUCER_INTERVAL_TIME = 200

    fun getAppConfigDir(): String {
        val dir = File(APP_CONFIG_PATH)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }



    fun getImgDir(): String {
        val dir = File(INTERNAL_CACHE_PATH,"Img")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }

    object SpParams {
        const val SERIAL_CONFIG = "serial_config"
        const val MQTT_CONFIG = "mqtt_config"

    }


}