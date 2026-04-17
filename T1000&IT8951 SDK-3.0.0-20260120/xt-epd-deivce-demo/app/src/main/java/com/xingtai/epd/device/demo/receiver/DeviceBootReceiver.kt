package com.xingtai.epd.device.demo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import com.sjl.util.LogWriter
import com.xingtai.epd.device.demo.ui.main.activity.MainActivity
import com.xingtai.epd.device.demo.util.AppConfigUtils
import com.xingtai.epd.device.demo.util.PagerRouter
import kotlin.system.exitProcess


/**
 * Device boot receiver.After the system starts, the app will automatically launch
 */
class DeviceBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (AppConfigUtils.isAutoBootApp()){
                rebootFlag = true
                PagerRouter.openActivity(context, MainActivity::class.java)
                LogWriter.e("\n\n\n\nDevice boot receiver...")
            }else{
                LogWriter.w("\n\n\n\nStartup has been disabled. Please go to the home screen to enable it.")
                // Kill the current process
                Process.killProcess(Process.myPid())
                exitProcess(0)
            }

        } else if (Intent.ACTION_SHUTDOWN == intent.action) {
        }
    }

    companion object {
        var rebootFlag = false
    }
}
