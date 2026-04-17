package com.xingtai.epd.device.demo.util

import androidx.core.util.Consumer

/**
 * Polling tool class
 *
 * @author Kelly
 * @version 1.0.0
 * @filename PollUtils
 * @time 2021/6/16 10:22
 * @copyright(C) 2021 song
 */
object PollUtils {

    /**
     *  Polling for specified time (blocked)
     *
     * @param duration
     * @param runnable
     */
    @JvmOverloads
    fun pollTime(duration: Long, runnable: Runnable?, consumer: Consumer<*>? = null) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start <= duration) {
            runnable?.run()
        }
        consumer?.accept(null)
    }

    /**
     * Polling for specified time (blocked, successfully exited)
     *
     * @param duration
     * @param consumer
     * @return
     */
    fun pollTimeWithJump(duration: Long, consumer: MyCallable<Boolean>): Boolean {
        val start = System.currentTimeMillis()
        var ret = false
        while (System.currentTimeMillis() - start <= duration) {
            ret = consumer.call()
            if (ret) {
                break
            }
        }
        return ret
    }

    interface MyCallable<V> {
        fun call(): V
    }
}
