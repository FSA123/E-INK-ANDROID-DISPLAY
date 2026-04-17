package com.xingtai.epd.device.demo.t1000

import com.sjl.util.LogUtils
import com.xingtai.device.t1000.entity.ScreenType
import com.xingtai.epd.device.demo.service.T1000Service
import com.xingtai.epd.device.demo.util.AppConfigUtils


/**
 * @author Kelly
 * @version 1.0.0
 * @filename T1000HelperFactory
 * @time 2023/11/30 14:14
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
object T1000HelperFactory {
    private var abstractT1000: AbstractT1000Helper? = null
    val instance: AbstractT1000Helper?
        get() {
            if (abstractT1000 == null) {
                synchronized(T1000Helper::class.java) {
                    if (abstractT1000 == null) {
                        val screenType = AppConfigUtils.screenType
                        init(screenType)
                    }
                }
            }
            return abstractT1000
        }

    private fun init(screenType: ScreenType?) {
        if (isMultiScreen(screenType)) {
            return
        } else {
            abstractT1000 = T1000Helper()
            (abstractT1000 as T1000Helper).screenType = screenType
        }

    }

    /**
     * Reset instance
     */
    fun reset(screenType: ScreenType) {
        abstractT1000?.let {
            it.screenType = screenType
            //Prevent duplicate instance creation
            if (!isMultiScreen(screenType) && it is T1000Helper) {
                return
            }
            it.disconnect()
            init(screenType)
            T1000Service.abstractT1000 = abstractT1000
            LogUtils.i("Reset instance successfully")
        }

    }

    private fun isMultiScreen(screenType: ScreenType?): Boolean {
        return ScreenType.isMulti8951Screen(screenType)
    }
}
