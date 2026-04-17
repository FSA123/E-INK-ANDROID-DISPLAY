package com.xingtai.epd.device.demo.entity

import com.xingtai.device.t1000.entity.ScreenType

/**
 * app config
 *
 * @author Kelly
 * @version 1.0.0
 * @filename AppConfig
 * @time 2023/8/11 13:46
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
class AppConfig {
    /**
     * Screen type
     *
     */
    var type = ScreenType.SCREEN_25_3_E6V3.type

    /**
     * Battery type
     * @see BatteryType
     */
    var batteryType: String? = null

    /**
     * Is the front light turned on when the device is turned on
     */
    var autoOpenLight = true


    /**
     * App auto-launch on startup, default true
     */
    var autoBootApp = true
}
