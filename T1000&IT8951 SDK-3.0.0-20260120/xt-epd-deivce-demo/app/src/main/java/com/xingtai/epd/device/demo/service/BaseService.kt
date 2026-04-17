package com.xingtai.epd.device.demo.service

import android.app.Service
import com.xingtai.epd.device.demo.entity.MessageEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Service base class
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BaseService
 * @time 2024/5/13 8:54
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
abstract class BaseService : Service() {
    override fun onCreate() {
        super.onCreate()
        ServiceManager.put(this)
        EventBus.getDefault().register(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent<*>) {
        _onMessageEvent(event)
    }

    protected fun _onMessageEvent(event: MessageEvent<*>) {}
    override fun onDestroy() {
        super.onDestroy()
        ServiceManager.remove(this)
        EventBus.getDefault().unregister(this)
    }

    /**
     * Release resources
     */
    open fun release() {

    }
}
