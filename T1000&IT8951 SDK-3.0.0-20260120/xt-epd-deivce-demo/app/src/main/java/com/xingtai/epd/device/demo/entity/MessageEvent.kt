package com.xingtai.epd.device.demo.entity

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MessageEvent
 * @time 2022/6/7 15:26
 * @copyright(C) 2022 song
 */
class MessageEvent<T>(val code: Int, val flag: String?, val event: T) {

    class Builder<T> {
        private var code = 0
        private var flag: String? = null
        private var event: T? = null
        fun setCode(code: Int): Builder<*> {
            this.code = code
            return this
        }

        fun setFlag(flag: String?): Builder<*> {
            this.flag = flag
            return this
        }

        fun setEvent(event: T): Builder<*> {
            this.event = event
            return this
        }

        fun create(): MessageEvent<*> {
            return MessageEvent(code, flag, event)
        }
    }
}