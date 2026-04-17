package com.xingtai.epd.device.demo.t1000

import android.hardware.usb.UsbDevice
import com.xingtai.epd.device.demo.t1000.entity.EpdImage
import com.xingtai.epd.device.demo.t1000.listener.T1000DisplayListener
import com.xingtai.epd.device.demo.t1000.listener.T1000InitListener

import java.io.File

/**
 * Universal interface
 *
 * @author Kelly
 * @version 1.0.0
 * @filename IT1000Helper
 * @time 2023/11/30 11:50
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
interface IT1000Helper {
    /**
     * USB device initialization
     * @param usbDevice
     * @param t1000InitListener
     */
    fun init(usbDevice: UsbDevice, t1000InitListener: T1000InitListener)

    /**
     * connect
     * @return
     */
    fun open(): Int

    /**
     * Connection status
     * @return
     */
    val state: Int

    /**
     * Update firmware
     * @param otaFile
     */
    fun upgradeFirmware(otaFile: File)

    /**
     * Update waveform
     * @param otaFile
     */
    fun upgradeWbf(otaFile: File)

    /**
     * Close connection
     */
    fun close()

    /**
     * Display image
     * @param epdImage
     * @param t1000DisplayListener
     */
    fun display(epdImage: EpdImage, t1000DisplayListener: T1000DisplayListener)

    /**
     * Initialize is used to completely clear the display, if it's left in an unknown state (i.e. if the previous image has been lost by a re-boot)
     */
    fun initScreen(startX: Int, startY: Int, width: Int, height: Int)

    /**
     * Activate image consumption display
     */
    fun startDisplay()

    /**
     * Stop displaying, disconnect completely to release resources
     */
    fun disconnect()
}
