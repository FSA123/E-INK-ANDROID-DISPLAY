package com.xingtai.epd.device.demo.t1000.listener

/**
 *
 *
 * @author Kelly
 * @version 1.0.0
 * @filename T1000InitListener
 * @time 2023/4/19 11:17
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
interface T1000InitListener {
    /**
     *
     * @param isLast Is it the last device
     */
    fun onSuccess(isLast: Boolean)
    fun onFail(e: Exception) {}
}
