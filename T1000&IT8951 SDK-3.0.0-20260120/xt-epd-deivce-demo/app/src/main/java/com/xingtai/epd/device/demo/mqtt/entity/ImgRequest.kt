package com.xingtai.epd.device.demo.mqtt.entity

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ImgRequest
 * @time 2024/9/6 9:21
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
class ImgRequest {
    /**
     *  {
     *   "action": "sendImg",
     *   "devId": "13b0b4eb2e717b9e",
     *   "url": "https://p6.itc.cn/images01/20201111/526473ac93954907a11fda0e21940b42.jpeg",
     *   "startX":0,
     *   "startY":0,
     *   "width":3200,
     *   "height":1800,
     *   "intervalTime":30,
     *   "displayMode":0
     * }
     *
     */
    var action: String? = null
    var devId: String? = null
    var url: String = ""
    var startX = 0
    var startY = 0
    var width = 0
    var height = 0
    var displayMode = 0

    var intervalTime = 0f

    companion object {
        /** Action value that triggers the image-download display flow. */
        const val ACTION_SEND_IMG = "sendImg"
    }
}
