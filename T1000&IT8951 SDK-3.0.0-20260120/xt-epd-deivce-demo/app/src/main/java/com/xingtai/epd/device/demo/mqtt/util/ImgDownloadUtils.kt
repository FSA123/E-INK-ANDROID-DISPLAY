package com.xingtai.epd.device.demo.mqtt.util

import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadSampleListener
import com.liulishuo.filedownloader.FileDownloader
import com.sjl.util.FileUtils
import com.sjl.util.LogUtils
import com.sjl.util.LogWriter
import com.xingtai.epd.device.demo.app.AppConstant.getImgDir
import com.xingtai.epd.device.demo.mqtt.entity.ImgRequest
import com.xingtai.epd.device.demo.mqtt.listener.DownloadCallback
import com.xingtai.epd.device.demo.t1000.T1000HelperFactory
import com.xingtai.epd.device.demo.t1000.entity.EpdImage
import java.io.File

object ImgDownloadUtils {

    fun download(imgRequest: ImgRequest, downloadCallback: DownloadCallback) {
        val url = imgRequest.url
        val fileUrl = url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        val saveDir = getImgDir()
        val filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1)
        val file = File(saveDir, filename)
        val startTime = System.currentTimeMillis()
        FileDownloader.getImpl().create(url)
            .setAutoRetryTimes(2) //失败重试2次
            .setPath(file.absolutePath)
            .setListener(object : FileDownloadSampleListener() {
                override fun progress(task: BaseDownloadTask, soFarBytes: Int, totalBytes: Int) {

                    if (totalBytes > 0){
                        val speed = task.speed
                        LogUtils.i(task.filename + ",size：" + FileUtils.formatFileSize(totalBytes.toLong()) + ",progress:" + (soFarBytes * 100 / totalBytes * 1.0).toInt() + "%" + ",speed:" + FileUtils.formatFileSize(
                            (speed * 1024).toLong()) + "/s")
                    }

                }

                override fun retry(task: BaseDownloadTask,
                                   ex: Throwable,
                                   retryingTimes: Int,
                                   soFarBytes: Int) {
                    LogWriter.w("Download retry： soFarBytes:" + soFarBytes + "--ex" + ex.message)
                }

                override fun completed(task: BaseDownloadTask) {
                    LogWriter.i(task.filename + "Download successful, time-consuming：" + (System.currentTimeMillis() - startTime) / 1000.0 + "s,path：" + task.targetFilePath)
                    downloadCallback.onSuccess()
                    val epdImage = EpdImage(file, imgRequest.intervalTime)
                    epdImage.startX = imgRequest.startX
                    epdImage.startY = imgRequest.startY
                    epdImage.width = imgRequest.width
                    epdImage.height = imgRequest.height
                    epdImage.displayMode = imgRequest.displayMode

                    epdImage.name = file.name
                    T1000HelperFactory.instance?.run {
                        addImage(epdImage)
                        requestSleepInterrupted()

                    }
                }

                override fun error(task: BaseDownloadTask, e: Throwable) {
                    LogWriter.e("Download error:$url", e)
                    downloadCallback.onError(e)
                }
            }).start()
    }
}
