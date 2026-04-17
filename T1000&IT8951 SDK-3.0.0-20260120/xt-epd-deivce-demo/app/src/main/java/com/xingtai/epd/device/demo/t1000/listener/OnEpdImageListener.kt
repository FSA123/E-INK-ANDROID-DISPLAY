package com.xingtai.epd.device.demo.t1000.listener

interface OnEpdImageListener<T> {
    fun onEpdImageChange(t: T)
}