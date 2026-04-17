package com.xingtai.epd.device.demo.util

import android.os.Build
import android.text.TextUtils

object DeviceUtils {
    val deviceId: String
        get() = Build.SERIAL

    /**
     * Determine whether the current device model is A40I
     * @return
     */
    fun isSmtDevice(): Boolean {
        //Device model
        val model = Build.MODEL
        return !TextUtils.isEmpty(model) && model.toUpperCase().contains("A40I")
    }
}
