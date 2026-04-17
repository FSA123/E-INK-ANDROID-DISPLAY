package com.xingtai.epd.device.demo.util

object CollectionUtils {
    fun isEmpty(coll: Collection<*>?): Boolean {
        return coll == null || coll.isEmpty()
    }

    fun isNotEmpty(coll: Collection<*>?): Boolean {
        return !isEmpty(coll)
    }
}
