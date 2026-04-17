package com.xingtai.epd.device.demo.service.listener

/**
 * Service listening
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ServiceListener
 * @time 2023/6/20 11:10
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
interface ServiceListener {
    /**
     * Service destruction callback
     */
    fun onDestroy()
}
