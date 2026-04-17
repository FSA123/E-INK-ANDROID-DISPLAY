package com.xingtai.epd.device.demo.entity

import android.text.TextUtils

/**
 * Battery type
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BatteryType
 * @time 2023/10/18 9:52
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
enum class BatteryType(
    /**
     * Pack type
     */
    val packType: String,
    /**
     * battery capacity（mAh）
     */
    val batteryCapacity: Int,
    /**
     * Nominal voltage
     */
    val nominalVoltage: Float,
    /**
     * Fully charge voltage FC
     */
    val fullyChargeVoltage: Float,
    /**
     * 满放电压 FD
     */
    val fullyDischargeVoltage: Float) {
    /**
     * Low battery protection threshold: 10.55-10.5
     */
    BATTERY_3S1P("3S1P", 5200, 11.4f, 13.05f, 9.0f),

    /**
     * Low battery protection threshold:13.55-13.5
     */
    BATTERY_4S1P("4S1P", 3500, 14.4f, 16.8f, 10.8f),

    /**
     * Direct power supply
     */
    BATTERY_DIRECT("Direct", -1, -1f, -1f, -1f);

    companion object {
        /**
         * Search for screen type based on type
         *
         * @param packType
         * @return
         */
        @JvmStatic
        fun parseByType(packType: String?): com.xingtai.epd.device.demo.entity.BatteryType? {
            var batteryType: com.xingtai.epd.device.demo.entity.BatteryType? = null
            for (item in values()) {
                if (TextUtils.equals(packType, item.packType)) {
                    batteryType = item
                    break
                }
            }
            return batteryType
        }
    }
}