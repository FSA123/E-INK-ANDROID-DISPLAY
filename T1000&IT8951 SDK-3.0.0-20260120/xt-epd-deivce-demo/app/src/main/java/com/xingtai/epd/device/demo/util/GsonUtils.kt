package com.xingtai.epd.device.demo.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.lang.reflect.Type

/**
 *
 *
 * @author Kelly
 * @version 1.0.0
 * @filename GsonUtils
 * @time 2022/6/14 11:13
 * @copyright(C) 2022 song
 */
object GsonUtils {
    var gson: Gson = GsonBuilder().disableHtmlEscaping().setDateFormat("yyyy-MM-dd HH:mm:ss").create()


    fun <T> fromJson(jsonStr: String, tClass: Class<T>): T {
        return gson.fromJson(jsonStr, tClass)
    }

    fun <T> fromJson(jsonStr: String, type: Type): T {
        return gson.fromJson(jsonStr, type)
    }

    fun toJson(`object`: Any): String {
        return gson.toJson(`object`)
    }

    fun beanToMap(o: Any?): Map<*, *> {
        if (o == null) {
            return HashMap<Any, Any>()
        }
        val json = gson.toJson(o)
        return gson.fromJson<Map<*, *>>(json, MutableMap::class.java)
    }
}
