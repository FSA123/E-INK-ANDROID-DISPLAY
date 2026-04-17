package com.xingtai.epd.device.demo.mcu.entity

class SerialConfig {
    @JvmField
    var devicePath: String? = null

    companion object {
        const val DEVICE_PATH_YF = "/dev/ttyS3"
        const val DEVICE_PATH_SMT = "/dev/ttyS2"
    }
}
