package com.xingtai.epd.device.demo.t1000.util

import com.xingtai.epd.device.demo.t1000.entity.EpdImage

/**
 * EpdImageWrap
 */
class EpdImageWrap {
    /**
     * Online resources or USB resources (second level cache)
     */
     val epdImages: MutableList<EpdImage> = ArrayList()

    /**
     * This index is a self increasing index after the first level cache (quantity 2) is full
     * @return
     */
    var currentPlayIndex = -1
    val nextEpdImage: EpdImage?
        /**
         * Get the next image
         * @return
         */
        get() {
            if (epdImages.size == 0) {
                return null
            }
            synchronized(epdImages) {
                if (currentPlayIndex >= epdImages.size) {
                    currentPlayIndex = 0
                }
                return epdImages[currentPlayIndex]
            }
        }

    /**
     * Every successful callback, auto increment index
     */
    fun autoIncrementCurrentPlayIndex() {
        synchronized(epdImages) {
            if (currentPlayIndex == epdImages.size - 1) {
                currentPlayIndex = 0
            } else {
                currentPlayIndex++
            }
        }
    }

    /**
     * Reset the resources being played
     * @param epdImages
     */
    fun resetEpdImage(epdImages: List<EpdImage>) {
        synchronized(this.epdImages) {
            clearEpdImage()
            this.epdImages.addAll(epdImages)
        }
    }

    /**
     * Add image
     * @param epdImages
     */
    fun addEpdImage(epdImages: EpdImage) {
        synchronized(this.epdImages) { this.epdImages.add(epdImages) }
    }

    /**
     * Clear image
     */
    fun clearEpdImage() {
        synchronized(epdImages) {
            epdImages.clear()
            currentPlayIndex = 0
        }
    }


    fun size(): Int {
        return epdImages.size
    }

    fun remove(nextEpdImage: EpdImage?): Boolean {
        synchronized(epdImages) {
            return if (epdImages.indexOf(nextEpdImage) >= 0) {
                epdImages.remove(nextEpdImage)
                if (currentPlayIndex > 0) {
                    currentPlayIndex--
                }
                true
            } else {
                false
            }
        }
    }
}
