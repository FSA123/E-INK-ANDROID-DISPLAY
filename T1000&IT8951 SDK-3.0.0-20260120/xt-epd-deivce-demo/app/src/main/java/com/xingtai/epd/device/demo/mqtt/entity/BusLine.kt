package com.xingtai.epd.device.demo.mqtt.entity

/**
 * A single bus arrival line in a [BusTextRequest].
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BusLine
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
class BusLine {
    var busNumber: String? = null
    var destinationStopName: String? = null
    var arrivalTime: String? = null
}
