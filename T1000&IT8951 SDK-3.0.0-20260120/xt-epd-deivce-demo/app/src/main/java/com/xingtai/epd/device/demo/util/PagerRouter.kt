package com.xingtai.epd.device.demo.util

import android.content.Context
import android.content.Intent

/**
 * Page routing management
 *
 * @author Kelly
 * @version 1.0.0
 * @filename PagerRouter
 * @time 2022/7/12 21:40
 * @copyright(C) 2022 song
 */
object PagerRouter {
    /**
     * 打开Activity
     *
     * @param context
     * @param cls
     */
    fun openActivity(context: Context?, cls: Class<*>?) {
        if (null == context || null == cls) {
            return
        }
        val i = Intent(context, cls)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}
