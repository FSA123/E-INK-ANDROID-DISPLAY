package com.xingtai.epd.device.demo.util

import java.lang.reflect.ParameterizedType

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename TUtils
 * @time 2022/6/7 11:47
 * @copyright(C) 2022 song
 */
object TUtils {
    fun getClass(clz: Class<*>): Class<*> {
        val type =
            clz.genericSuperclass as ParameterizedType
        return type.actualTypeArguments[0] as Class<*>
    }
}
