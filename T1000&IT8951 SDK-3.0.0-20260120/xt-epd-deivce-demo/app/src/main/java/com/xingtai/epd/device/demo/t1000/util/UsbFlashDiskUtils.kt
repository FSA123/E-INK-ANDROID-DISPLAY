package com.xingtai.epd.device.demo.t1000.util

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.os.Build
import com.google.gson.reflect.TypeToken
import com.sjl.util.FileUtils
import com.sjl.util.LogWriter
import com.xingtai.epd.device.demo.t1000.T1000HelperFactory
import com.xingtai.epd.device.demo.t1000.entity.EpdImage
import com.xingtai.epd.device.demo.util.GsonUtils
import java.io.File

/**
 *
 *
 * @author Kelly
 * @version 1.0.0
 * @filename UsbFlashDiskUtils
 * @time 2023/4/10 10:43
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
object UsbFlashDiskUtils {
    /**
     * Is it a USB flash disk
     * @param device
     * @return
     */
    fun isUsbFlashDisk(device: UsbDevice): Boolean {
        if (device.interfaceCount != 1) {
            return false
        }
        val usbInterface = device.getInterface(0)
        if (usbInterface.interfaceClass != UsbConstants.USB_CLASS_MASS_STORAGE) {
            return false
        }
        if (usbInterface.interfaceSubclass != 0x06 && usbInterface.interfaceSubclass != 0x05) {
            return false
        }
        if (usbInterface.interfaceProtocol != 0x50 && usbInterface.interfaceProtocol != 0x01) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //The T1000 is similar to a USB flash disk and needs to be ignored
            if (device.manufacturerName!!.contains("ITE") || device.productName!!.contains("ITE")) {
                return false
            }
        }
        return true
    }

    fun parseConfig(file: File): Boolean {
        try {
            val jsonConfigFile = File(file, "slideShowImg.json")
            val str = FileUtils.readFileToString(jsonConfigFile)
            val epdImages: List<EpdImage> = GsonUtils.gson.fromJson(str, object : TypeToken<List<EpdImage>>() {}.type)
            var imgDir = File(file, "image")
            if (!imgDir.exists()){
                return false
            }
            for (epdImage in epdImages){
                epdImage.file = File(imgDir, epdImage.name)
            }
            T1000HelperFactory.instance?.initEpdImage(epdImages)
        } catch (e: Exception) {
            LogWriter.e("Abnormal parsing of USB resource package",e)
            return false
        }
        return true
    }
}
