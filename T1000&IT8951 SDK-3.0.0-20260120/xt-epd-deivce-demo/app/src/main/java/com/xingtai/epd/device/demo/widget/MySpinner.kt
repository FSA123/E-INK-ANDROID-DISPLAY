package com.xingtai.epd.device.demo.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnTouchListener
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatSpinner

/**
 *
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MySpinner
 * @time 2024/1/9 18:16
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
class MySpinner : AppCompatSpinner {
    private var isFirst = true
    private var spinnerItemSelectedListener: SpinnerItemSelectedListener? = null

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, mode: Int) : super(context!!, mode) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs) {
        init()
    }

    private fun init() {
        //Resolve the spinner's item click event, which can only be clicked once
        setOnTouchListener(OnTouchListener { v, event ->
            try {
                val clazz: Class<*> = AdapterView::class.java
                val field = clazz.getDeclaredField("mOldSelectedPosition")
                field.isAccessible = true
                field.setInt(this@MySpinner, INVALID_POSITION)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            false
        })
        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View,
                                        position: Int,
                                        id: Long) {
                //解决spinner第一次不被选中
                if (isFirst) {
                    isFirst = false
                    return
                }
                if (spinnerItemSelectedListener != null) {
                    spinnerItemSelectedListener!!.onItemSelected(parent, view, position, id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (spinnerItemSelectedListener != null) {
                    spinnerItemSelectedListener!!.onNothingSelected(parent)
                }
            }
        }
    }

    fun setSpinnerItemSelectedListener(spinnerItemSelectedListener: SpinnerItemSelectedListener?) {
        this.spinnerItemSelectedListener = spinnerItemSelectedListener
    }
}
