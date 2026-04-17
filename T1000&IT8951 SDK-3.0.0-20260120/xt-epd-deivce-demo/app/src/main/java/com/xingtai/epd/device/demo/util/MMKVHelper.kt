package com.xingtai.epd.device.demo.util

import android.os.Parcelable
import com.tencent.mmkv.MMKV

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MMKVHelper
 * @time 2023/3/21 11:16
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
object MMKVHelper {
    private val smMKV = MMKV.defaultMMKV()

    /**
     * To save data, we need to obtain the specific type of data to be saved, and then call different saving methods based on the type
     */
    fun encode(key: String?, `object`: Any) {
        smMKV?.run {
            if (`object` is String) {
                encode(key, `object`)
            } else if (`object` is Int) {
                encode(key, `object`)
            } else if (`object` is Boolean) {
                encode(key, `object`)
            } else if (`object` is Float) {
                encode(key, `object`)
            } else if (`object` is Long) {
                encode(key, `object`)
            } else if (`object` is Double) {
                encode(key, `object`)
            } else if (`object` is ByteArray) {
                encode(key, `object`)
            } else {
                encode(key, `object`.toString())
            }
        }

    }


    fun decodeInt(key: String?): Int {
        return smMKV!!.decodeInt(key, 0)
    }

    fun decodeDouble(key: String?): Double {
        return smMKV!!.decodeDouble(key, 0.00)
    }

    fun decodeLong(key: String?): Long {
        return smMKV!!.decodeLong(key, 0L)
    }

    fun decodeBoolean(key: String?): Boolean {
        return smMKV!!.decodeBool(key, false)
    }

    fun decodeFloat(key: String?): Float {
        return smMKV!!.decodeFloat(key, 0f)
    }

    fun decodeBytes(key: String?): ByteArray? {
        return smMKV!!.decodeBytes(key)
    }

    fun decodeString(key: String?): String {
        return smMKV?.decodeString(key, "") ?: ""
    }

    fun decodeStringSet(key: String?): Set<String>? {
        return smMKV!!.decodeStringSet(key, emptySet())
    }

    fun decodeParcelable(key: String?): Parcelable? {
        return smMKV!!.decodeParcelable(key, null)
    }

    /**
     * Remove a key pair
     * @param key
     */
    fun removeValueForKey(key: String) {
        smMKV?.removeValueForKey(key)
    }

    /**
     * Simultaneously removing multiple key pairs
     * @param strings
     */
    fun removeValuesForKeys(strings: Array<String>) {
        smMKV?.removeValuesForKeys(strings)
    }

    /**
     * Clear all keys
     */
    fun clearAll() {
        smMKV?.clearAll()
    }


}
