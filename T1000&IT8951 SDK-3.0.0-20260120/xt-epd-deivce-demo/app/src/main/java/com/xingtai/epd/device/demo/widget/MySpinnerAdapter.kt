package com.xingtai.epd.device.demo.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.xingtai.epd.device.demo.R

/**
 *
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MySpinnerAdapter
 * @time 2024/8/7 14:54
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
class MySpinnerAdapter @JvmOverloads constructor(context: Context,
                                                 resource: Int,
                                                 data: List<String>,
                                                 private val textSize: Int = (context.resources.getDimensionPixelSize(
                                                     R.dimen.sp_12) / context.resources.displayMetrics.density).toInt()) :
    ArrayAdapter<String?>(context, resource, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.textSize = textSize.toFloat()
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        view.textSize = textSize.toFloat()
        return view
    }
}