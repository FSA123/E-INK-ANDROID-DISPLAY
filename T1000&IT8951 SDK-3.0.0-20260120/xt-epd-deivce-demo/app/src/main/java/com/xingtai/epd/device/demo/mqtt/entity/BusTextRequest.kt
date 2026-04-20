package com.xingtai.epd.device.demo.mqtt.entity

/**
 * MQTT payload for text-based bus arrival display.
 *
 * Expected JSON format:
 * {
 *   "action": "sendBusInfo",
 *   "devId": "13b0b4eb2e717b9e",
 *   "displayMode": 0,
 *   "lines": [
 *     {"busNumber": "42",  "arrivalTime": "2 min"},
 *     {"busNumber": "17",  "arrivalTime": "5 min"},
 *     {"busNumber": "8",   "arrivalTime": "12 min"},
 *     {"busNumber": "23",  "arrivalTime": "Arriving"},
 *     {"busNumber": "101", "arrivalTime": "18 min"}
 *   ]
 * }
 *
 * Rules:
 * - At most 5 lines are displayed; extras are ignored.
 * - Missing lines are rendered as blank bands.
 * - Overlong text is truncated with an ellipsis.
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BusTextRequest
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
class BusTextRequest {
    var action: String? = null
    var devId: String? = null
    var displayMode: Int = 0
    var lines: List<BusLine>? = null

    companion object {
        /** Action value that triggers the bus-text display flow. */
        const val ACTION_SEND_BUS_INFO = "sendBusInfo"
    }
}
