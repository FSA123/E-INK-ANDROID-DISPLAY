package com.xingtai.epd.device.demo.sys

import android.os.Build
import com.xingtai.sys.api.EmptySysApi
import com.xingtai.sys.api.SysApi
import com.xingtai.sys.api.smt40a.Smt40aApi
import com.xingtai.epd.device.demo.app.MyApplication.Companion.getContext
import com.xingtai.epd.device.demo.util.DeviceUtils

/**
 *
 *
 * @author Kelly
 * @version 1.0.0
 * @filename SysApiFactory
 * @time 2023/4/26 13:56
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
object SysApiFactory {
    var sysApi: SysApi? = null
        /**
         * Obtain API for specific systems
         *
         * @return
         */
        get() {
            if (field != null) {
                return field
            }
            val context = getContext()
            field =
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1 && DeviceUtils.isSmtDevice()) {
                    //7.1.1
                    Smt40aApi(context)
                } else {
                    EmptySysApi(context)
                }
            return field
        }
        private set
}
