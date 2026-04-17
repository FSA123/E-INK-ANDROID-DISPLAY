package com.xingtai.epd.device.demo.t1000.listener

/**
 * T1000 display listening
 *
 * @author Kelly
 * @version 1.0.0
 * @filename T1000DisplayListener
 * @time 2023/3/31 16:53
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
interface T1000DisplayListener {
    fun onSuccess()
    fun onFail(e: Exception)
}
