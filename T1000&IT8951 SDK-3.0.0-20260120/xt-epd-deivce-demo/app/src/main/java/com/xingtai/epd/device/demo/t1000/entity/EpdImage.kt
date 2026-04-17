package com.xingtai.epd.device.demo.t1000.entity

import android.text.TextUtils
import java.io.File

/**
 * EpdImage
 *
 * @author Kelly
 * @version 1.0.0
 * @filename EpdImage
 * @time 2023/4/10 11:02
 * @copyright(C) 2023 江西兴泰科技股份有限公司
 */
class EpdImage : Cloneable {
    var name: String? = null
    var desc: String? = null

    /**
     * file
     */
    var file: File? = null
    var bytes: ByteArray? = null
    var displayMode = 0


    var startX = 0
    var startY = 0
    var width = 0
    var height = 0

    /**
     * Unit second
     */
    var intervalTime = 0f

    /**
     * Is it displayed every time the screen is cleared?
     */
    var isClearDisplay = false

    /**
     * 0:Image file
     */
    var formatType = FORMAT_TYPE_FILE

    constructor()
    constructor(file: File?, intervalTime: Float) {
        this.file = file
        this.intervalTime = intervalTime
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): EpdImage {
        return super.clone() as EpdImage
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val epdImage = o as EpdImage
        return if (!TextUtils.equals(name, epdImage.name)) false else file == epdImage.file
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + file.hashCode()
        return result
    }

    override fun toString(): String {
        return "{" +
                "name='" + name + '\'' +
                ", file=" + file +
                '}'
    }

    companion object {
        /**
         * Image file
         */
        const val FORMAT_TYPE_FILE = 0
        /**
         * Image bitmap
         */
        const val FORMAT_TYPE_BITMAP = 1

        /**
         * Image byte
         */
        const val FORMAT_TYPE_BYTE = 2
    }
}
