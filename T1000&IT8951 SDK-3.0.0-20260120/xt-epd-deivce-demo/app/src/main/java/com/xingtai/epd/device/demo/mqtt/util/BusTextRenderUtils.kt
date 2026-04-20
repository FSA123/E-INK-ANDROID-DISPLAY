package com.xingtai.epd.device.demo.mqtt.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.sjl.util.LogWriter
import com.xingtai.epd.device.demo.app.AppConstant
import com.xingtai.epd.device.demo.mqtt.entity.BusLine
import com.xingtai.epd.device.demo.mqtt.entity.BusTextRequest
import com.xingtai.epd.device.demo.t1000.T1000HelperFactory
import com.xingtai.epd.device.demo.t1000.entity.EpdImage
import com.xingtai.epd.device.demo.util.AppConfigUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a bus-arrival text image from a [BusTextRequest] and enqueues it for display.
 *
 * Layout (5 horizontal bands, full display resolution):
 *   Band 0 (and 2, 4): background BLACK  / text WHITE
 *   Band 1 (and 3):    background WHITE  / text BLACK
 *
 * Each band shows one bus line: bus number on the left, arrival time on the right.
 * Overlong text is truncated with an ellipsis so it never overflows its half of the band.
 * If fewer than 5 lines are supplied the remaining bands are rendered as empty (no text).
 * If more than 5 lines are supplied only the first 5 are used.
 *
 * Default target resolution is 2560 × 1440 (31.2" EC), automatically adjusted
 * from the configured [AppConfigUtils.screenType] when valid.
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BusTextRenderUtils
 * @copyright(C) 2024 江西兴泰科技股份有限公司
 */
object BusTextRenderUtils {

    private const val MAX_LINES = 5
    private const val DEFAULT_WIDTH = 2560
    private const val DEFAULT_HEIGHT = 1440
    private const val MAX_CACHED_BUS_IMAGES = 20

    /**
     * Generate the bus-text image from [request], save it to the image cache directory,
     * wrap it in an [EpdImage] and enqueue it for immediate display.
     */
    fun render(request: BusTextRequest) {
        try {
            val screenType = AppConfigUtils.screenType
            val width = if (screenType.width > 0) screenType.width else DEFAULT_WIDTH
            val height = if (screenType.height > 0) screenType.height else DEFAULT_HEIGHT

            val imgDir = AppConstant.getImgDir()

            val file = generateImage(request.lines ?: emptyList(), width, height, imgDir)
            if (!file.exists() || !file.canRead()) {
                LogWriter.e("BusTextRenderUtils: generated image is missing or unreadable: ${file.absolutePath}")
                return
            }

            val epdImage = EpdImage(file, 0f)
            epdImage.name = file.name
            epdImage.startX = 0
            epdImage.startY = 0
            epdImage.width = width
            epdImage.height = height
            epdImage.displayMode = request.displayMode
            epdImage.formatType = EpdImage.FORMAT_TYPE_FILE

            T1000HelperFactory.instance?.run {
                enqueueLatest(epdImage)
                requestSleepInterrupted()
            }
            pruneOldBusImages(imgDir)
            LogWriter.i("Bus text image enqueued: ${file.absolutePath}")
        } catch (e: Exception) {
            LogWriter.e("BusTextRenderUtils render error", e)
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun enqueueLatest(epdImage: EpdImage) {
        val helper = T1000HelperFactory.instance ?: return
        val offered = helper.addImage(epdImage)
        if (!offered) {
            LogWriter.w("BusTextRenderUtils: queue full, clear pending queue and enqueue latest bus image")
            helper.clearImage()
            val retryOffered = helper.addImage(epdImage)
            if (!retryOffered) {
                LogWriter.e("BusTextRenderUtils: enqueue failed even after queue reset")
            }
        }
    }

    /**
     * Keep only the latest generated bus-text files and remove old ones to avoid cache accumulation.
     */
    private fun pruneOldBusImages(imgDir: String) {
        try {
            val files = File(imgDir).listFiles { _, name ->
                name.startsWith("bus_text_") && name.endsWith(".png")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
            if (files.size <= MAX_CACHED_BUS_IMAGES) {
                return
            }
            files.drop(MAX_CACHED_BUS_IMAGES).forEach {
                if (!it.delete()) {
                    LogWriter.w("BusTextRenderUtils: failed to delete old cache file: ${it.absolutePath}")
                }
            }
        } catch (e: Exception) {
            LogWriter.w("BusTextRenderUtils: could not prune old bus images: " + e.message)
        }
    }

    /**
     * Draw the 5-band image into a Bitmap and persist it as a PNG file.
     *
     * @param lines   ordered list of bus lines (up to [MAX_LINES] are used)
     * @param width   image width in pixels
     * @param height  image height in pixels
     * @param imgDir  directory path where the PNG will be saved
     * @return        the saved [File]
     */
    private fun generateImage(
        lines: List<BusLine>,
        width: Int,
        height: Int,
        imgDir: String
    ): File {
        val bandHeight = height / MAX_LINES

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
            // Font size: 55 % of band height gives readable text on a large e-ink panel
            textSize = bandHeight * 0.55f
        }

        // Horizontal padding: 4 % of width on each side
        val hPad = (width * 0.04f).toInt()
        // Maximum width allocated to each text field (left: bus number, right: arrival time)
        val maxFieldWidth = (width * 0.44f).toInt()

        for (i in 0 until MAX_LINES) {
            val bandTop = i * bandHeight
            val bandBottom = bandTop + bandHeight

            // Even index → black background / white text
            // Odd  index → white background / black text
            val isOdd = i % 2 != 0
            val bgColor = if (isOdd) Color.WHITE else Color.BLACK
            val fgColor = if (isOdd) Color.BLACK else Color.WHITE

            // Draw band background
            paint.color = bgColor
            canvas.drawRect(0f, bandTop.toFloat(), width.toFloat(), bandBottom.toFloat(), paint)

            val line = lines.getOrNull(i)
            val busNumber = line?.busNumber?.trim()?.takeIf { it.isNotEmpty() } ?: ""
            val arrivalTime = line?.arrivalTime?.trim()?.takeIf { it.isNotEmpty() } ?: ""

            if (busNumber.isEmpty() && arrivalTime.isEmpty()) {
                continue
            }

            paint.color = fgColor

            // Vertical baseline: center the text block within the band
            val fm = paint.fontMetrics
            val textBlockHeight = fm.descent - fm.ascent
            val baselineY = bandTop + (bandHeight - textBlockHeight) / 2 - fm.ascent

            // Bus number – left-aligned
            if (busNumber.isNotEmpty()) {
                val display = truncateText(paint, busNumber, maxFieldWidth)
                canvas.drawText(display, hPad.toFloat(), baselineY, paint)
            }

            // Arrival time – right-aligned
            if (arrivalTime.isNotEmpty()) {
                val display = truncateText(paint, arrivalTime, maxFieldWidth)
                val textWidth = paint.measureText(display)
                canvas.drawText(display, width - hPad - textWidth, baselineY, paint)
            }
        }

        // Fill any remainder pixel rows (when height is not perfectly divisible by 5)
        val filledHeight = bandHeight * MAX_LINES
        if (filledHeight < height) {
            paint.color = Color.BLACK
            canvas.drawRect(0f, filledHeight.toFloat(), width.toFloat(), height.toFloat(), paint)
        }

        // Persist to file
        val filename = "bus_text_${System.currentTimeMillis()}.png"
        val file = File(imgDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        return file
    }

    /**
     * Truncate [text] so that it fits within [maxWidthPx] pixels at the current [paint] settings,
     * appending "…" when truncation is required.
     */
    private fun truncateText(paint: Paint, text: String, maxWidthPx: Int): String {
        if (paint.measureText(text) <= maxWidthPx) {
            return text
        }
        val ellipsis = "\u2026" // single-character ellipsis
        var result = text
        while (result.isNotEmpty() && paint.measureText(result + ellipsis) > maxWidthPx) {
            result = result.dropLast(1)
        }
        return result + ellipsis
    }
}
