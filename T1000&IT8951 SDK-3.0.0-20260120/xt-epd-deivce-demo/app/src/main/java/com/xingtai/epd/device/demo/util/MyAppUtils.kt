package com.xingtai.epd.device.demo.util

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import com.sjl.util.LogWriter
import java.io.File
import java.util.Collections
import java.util.concurrent.TimeUnit


/**
 * App tool class
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MyAppUtils
 * @time 2023/4/21 8:52
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
object MyAppUtils {
    fun isServiceRunning(context: Context, className: String): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (manager != null) {
            val runningServices = manager.getRunningServices(100)
            for (service in runningServices) {
                if (className.equals(service.service.className)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * @return null may be returned if the specified process not found
     */
    fun getProcessName(cxt: Context, pid: Int): String? {
        val am = cxt.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return null
        for (procInfo in runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName
            }
        }
        return null
    }

    /**
     * Sort files
     *
     * @param files
     * @param asc true:Ascending order，false:Descending order
     * @return
     */
    fun sortFiles(files: Array<File>, asc: Boolean): MutableList<File> {
        val rootFileLists = mutableListOf(*files)
        Collections.sort(rootFileLists, java.util.Comparator { o1, o2 ->
            if (o1 != null && o2 != null){
                if (o1.isDirectory && o2.isFile) return@Comparator -1
                if (o1.isFile && o2.isDirectory) return@Comparator 1
                if (asc) {
                    o1.name.compareTo(o2.name) //升序
                } else {
                    -o1.name.compareTo(o2.name) //降序
                }
            }else{
                return@Comparator -1
            }

        })
        return rootFileLists
    }

    fun isLandscape(context: Context): Boolean {
        //Vertical screen
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    }

    /**
     * flush cache,prevent data loss due to power outage
     *
     * @return
     */
    fun flushCache(): Boolean {
        var result = false
        try {
            val process = Runtime.getRuntime().exec("sync")
            if (process != null) {
                val i = process.waitFor()
                result = i == 0
                process.destroy()
            }
        } catch (e: Exception) {
            LogWriter.e("flushCache", e)
        }
        return result
    }

    /**
     * Format milliseconds for minutes and seconds
     * @param milliSeconds
     * @return
     */
    fun formatHms(milliSeconds: Long?): String {
        if (milliSeconds == null) {
            return "--"
        }
        val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(milliSeconds) % 60
        val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(milliSeconds) % 60
        val hours: Long = TimeUnit.MILLISECONDS.toHours(milliSeconds)
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
