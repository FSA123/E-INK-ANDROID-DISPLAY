package com.xingtai.epd.device.demo.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Service Manager
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ServiceManager
 * @time 2024/5/13 8:48
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
object ServiceManager {

    private val serviceMap: MutableMap<String, BaseService> = ConcurrentHashMap()

    fun put(baseService: BaseService) {
        serviceMap[baseService.javaClass.simpleName] = baseService
    }

    fun remove(baseService: BaseService) {
        serviceMap.remove(baseService.javaClass.simpleName)
    }

    operator fun get(baseService: BaseService): BaseService? {
        return serviceMap[baseService.javaClass.simpleName]
    }

    operator fun get(tag: String): BaseService? {
        return serviceMap[tag]
    }

}
