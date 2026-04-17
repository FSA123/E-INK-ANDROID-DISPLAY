package com.xingtai.epd.device.demo.t1000.listener

import com.xingtai.epd.device.demo.t1000.entity.EpdImage


/**
 * Image display change listening
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ImageDisplayChangeListener
 * @time 2023/5/5 9:52
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
interface ImageDisplayChangeListener {
    fun onSuccess(epdImage: EpdImage)
    fun onFail(e: Exception)
}
