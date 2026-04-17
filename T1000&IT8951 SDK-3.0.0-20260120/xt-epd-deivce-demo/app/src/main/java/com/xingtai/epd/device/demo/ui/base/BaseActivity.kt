package com.xingtai.epd.device.demo.ui.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 *
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BaseActivity
 * @time 2021/9/11 14:40
 * @copyright(C) 2021 song
 */
abstract class BaseActivity<VB : ViewBinding>(private val inflate: (LayoutInflater) -> VB) : AppCompatActivity() {
    protected lateinit var mViewBinding: VB
    protected lateinit var mContext: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        mViewBinding = inflate(layoutInflater)
        setContentView(mViewBinding.root)
        ready()
        initVM()
        initView()
        initListener()
        initData()
    }


    protected fun ready() {}
    protected fun initVM() {}
    protected abstract fun initView()
    protected abstract fun initListener()
    protected abstract fun initData()

    @JvmOverloads
    fun openActivity(clz: Class<*>, bundle: Bundle? = null) {
        val intent = Intent(mContext, clz)
        if (bundle != null) {
            intent.putExtras(bundle)
        }
        mContext.startActivity(intent)
    }



    companion object {
        /**
         * Determine whether the activity has been destroyed
         *
         * @param mActivity
         * @return
         */
        fun isDestroy(mActivity: Activity?): Boolean {
            return if (mActivity == null || mActivity.isFinishing || Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && mActivity.isDestroyed) {
                true
            } else {
                false
            }
        }
    }
}
