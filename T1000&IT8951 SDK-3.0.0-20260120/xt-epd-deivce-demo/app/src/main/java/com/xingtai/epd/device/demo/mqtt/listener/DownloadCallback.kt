package com.xingtai.epd.device.demo.mqtt.listener

/**
 * Download callback
 *
 * @author Kelly
 * @version 1.0.0
 * @filename DownloadCallback
 * @time 2024/8/29 9:24
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
interface DownloadCallback {
    fun onSuccess()
    fun onError(e: Throwable)
}
