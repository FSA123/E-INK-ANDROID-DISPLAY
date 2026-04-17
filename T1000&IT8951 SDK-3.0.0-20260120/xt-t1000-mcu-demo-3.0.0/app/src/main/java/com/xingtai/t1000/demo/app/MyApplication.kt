package com.xingtai.t1000.demo.app

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.multidex.MultiDex
import com.seekink.imgrender.e6.E6Shell
import com.sjl.util.LogUtils
import com.xingtai.device.mcu.Mcu
import com.xingtai.device.t1000.T1000
import com.xingtai.device.t1000.T1000Constant
import com.xingtai.device.t1000.TConConstant
import me.jessyan.autosize.AutoSizeConfig
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * @author song
 */
open class MyApplication : Application() {

    companion object {

        lateinit var instance: Context
        var version: String = ""
        var versionCode: Int = 0
        private val executorService = Executors.newCachedThreadPool()
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        fun getExecutor(): ExecutorService {
            return executorService
        }

        fun getMainThreadExecutor(): Handler {
            return mainThreadHandler
        }

        fun getContext(): Context {
            return instance
        }
    }
    init {
        instance = this
    }
    var isLogEnable = true
    private val mTag = "SIMPLE_LOGGER"

    override fun onCreate() {
        super.onCreate()
        try {
            val packageInfo: PackageInfo = packageManager.getPackageInfo(this.packageName, 0)
            version = packageInfo.versionName
            versionCode = packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        //Screen adaptation dynamic adjustment
        val sw: Int = resources.configuration.smallestScreenWidthDp
        val newConfig: Configuration = resources.configuration
        if (newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {

            if (sw in 200..500) {
                AutoSizeConfig.getInstance().designWidthInDp = 360
            } else {
                AutoSizeConfig.getInstance().designWidthInDp = (sw*0.8).toInt()
            }
        }else{
            AutoSizeConfig.getInstance().designWidthInDp = sw
        }

        try {
            E6Shell.initRender(this,null)
        } catch (e: Exception) {
            LogUtils.e("E6 rendering initialization failed.",e)
        }
        T1000.init(this,isLogEnable)
        T1000.setE6DitheringMethod(TConConstant.DITHERING_METHOD_EINK)
        //or T1000.setE6DitheringMethod(TConConstant.DITHERING_METHOD_SEEKINK)


        Mcu.init(this,isLogEnable)
        LogUtils.init(mTag,isLogEnable)
    }



    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}