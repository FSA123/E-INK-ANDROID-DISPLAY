package com.xingtai.epd.device.demo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sjl.util.LogWriter
import com.xingtai.epd.device.demo.app.AppConstant
import com.xingtai.epd.device.demo.app.MyApplication
import com.xingtai.epd.device.demo.mcu.McuHelper
import com.xingtai.epd.device.demo.t1000.util.UsbFlashDiskUtils
import com.xingtai.epd.device.demo.util.MyAppUtils
import com.xingtai.epd.device.demo.util.SpUtils
import java.io.File

/**
 * Media broadcasting, monitoring external storage mounting and uninstallation
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MediaReceiver
 * @time 2023/4/10 10:16
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
class MediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MEDIA_CHECKING -> {}
            Intent.ACTION_MEDIA_MOUNTED -> {

                // Get the mounting path and read the USB file
                val uri = intent.data
                if (uri != null) {
                    val filePath = uri.path
                    LogWriter.i("Detected USB disk mounting：$filePath")
                    val rootFile = File(filePath)
                    val files = rootFile.listFiles()
                    if (files == null || files.isEmpty()) {
                        return
                    }
                    val rootFileLists = MyAppUtils.sortFiles(files, true)
                    for (file in rootFileLists) {
                        // File List
                        val absolutePath = file.absolutePath
                        LogWriter.i(absolutePath)
                        // TODO:Implement your own business logic
                        if (absolutePath.endsWith(AppConstant.MY_RESOURCES_USB)) {
                            MyApplication.getExecutor().execute(Runnable {
                                val b: Boolean = UsbFlashDiskUtils.parseConfig(file)
                                if (!b) {
                                    LogWriter.e("Failed to parse USB resources\n")
                                }
                            })
                            break
                        }
                    }
                }
            }

            Intent.ACTION_MEDIA_EJECT -> {}
            Intent.ACTION_MEDIA_REMOVED -> {
                val uri = intent.data
                if (uri != null) {
                    val filePath = uri.path
                    LogWriter.i("Detected removal of USB flash drive：$filePath")
                }
            }

            Intent.ACTION_MEDIA_UNMOUNTED -> {
                val uri = intent.data
                if (uri != null) {
                    val filePath = uri.path
                    LogWriter.i("Detected USB disk uninstallation：$filePath")
                }
            }

            else -> {}
        }
    }
}
