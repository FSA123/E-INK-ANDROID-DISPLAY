package com.xingtai.epd.device.demo.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.multidex.MultiDex
import com.liulishuo.filedownloader.FileDownloader
import com.liulishuo.filedownloader.connection.FileDownloadUrlConnection
import com.seekink.imgrender.e6.E6Shell
import com.sjl.util.LogUtils
import com.sjl.util.LogWriter
import com.tencent.mmkv.MMKV
import com.xingtai.device.mcu.Mcu
import com.xingtai.device.t1000.T1000
import com.xingtai.device.t1000.TConConstant
import com.xingtai.epd.device.demo.util.MyAppUtils
import com.xingtai.epd.device.demo.util.NtpClient
import me.jessyan.autosize.AutoSizeConfig
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * @author song
 */
open class MyApplication : Application() {

    companion object {

        lateinit var instance: Context

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
        //Screen adaptation dynamic adjustment
        val sw: Int = resources.configuration.smallestScreenWidthDp
        val newConfig: Configuration = resources.configuration
        //Vertical screen
        if (newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {

            AutoSizeConfig.getInstance().designWidthInDp = 360
        }else{
            //Horizontal screen without adjusting screen density
            AutoSizeConfig.getInstance().designWidthInDp = sw
        }

        val packageName = packageName
        val processName = MyAppUtils.getProcessName(this, android.os.Process.myPid());
        if (processName != null) {
            if (processName == packageName) {
                initMyApp()
            }
        }
    }

    private fun initMyApp() {
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
        val initialize = MMKV.initialize(this)
        //        LogUtils.i(initialize)

        LogWriter.init(isLogEnable,mTag,this)

        FileDownloader.setupOnApplicationOnCreate(this).connectionCreator(FileDownloadUrlConnection.Creator(FileDownloadUrlConnection.Configuration()
            .connectTimeout(30_000) // set connection timeout.
            .readTimeout(30_000) // set read timeout.
                ))
        //Set the maximum number of parallel downloads
        FileDownloader.getImpl().setMaxNetworkThreadCount(3)
        NtpClient.start()
    }


    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}