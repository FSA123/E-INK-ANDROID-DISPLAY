package com.xingtai.epd.device.demo.mqtt.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.sjl.util.LogWriter
import com.xingtai.device.t1000.entity.ScreenType
import com.xingtai.epd.device.demo.app.AppConstant
import com.xingtai.epd.device.demo.mqtt.entity.BusLine
import com.xingtai.epd.device.demo.mqtt.entity.BusTextRequest
import com.xingtai.epd.device.demo.t1000.T1000HelperFactory
import com.xingtai.epd.device.demo.t1000.entity.EpdImage
import com.xingtai.epd.device.demo.util.AppConfigUtils
import com.xingtai.epd.device.demo.util.NtpClient
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Renders bus-arrival data onto the EPD and supports partial refresh.
 *
 * Layout — 8 equal rows (each = screen-width / 8, pre-rotation):
 *   Row 0      : header  — station name (left) + NTP time HH:mm (right)
 *   Rows 1–5   : bus lines — route name (left), arrival time (right)
 *   Rows 6–7   : footer  — blank, reserved for custom text
 *
 * Row colours alternate: even = BLACK bg / WHITE text, odd = WHITE bg / BLACK text.
 *
 * Partial refresh:
 *   On every MQTT update the cached state is compared row by row.
 *   Only changed rows are reloaded and refreshed (DU mode, ~0.3 s).
 *   The first render is always a full GC16 refresh.
 *   The header clock is refreshed every minute independently of MQTT traffic.
 *
 * Coordinate mapping (after -90° software rotation):
 *   Portrait row i  →  landscape strip  x = [i·unitH, (i+1)·unitH),  y = [0, screenH)
 */
object BusTextRenderUtils {

    private const val MAX_LINES   = 5
    private const val TOTAL_ROWS  = MAX_LINES + 3   // 8: 1 header + 5 bus + 2 footer
    private const val DEFAULT_W   = 2560
    private const val DEFAULT_H   = 1440
    private const val MAX_CACHED  = 20
    private const val FILE_PREFIX = "bus_epd_"

    // ── Display-state cache ──────────────────────────────────────────────────

    private data class CachedState(
        val stationName: String,
        val labels:      List<String>,   // size MAX_LINES
        val arrivals:    List<String>,   // size MAX_LINES
        val unitH:       Int,
        val canvasW:     Int,   // portrait canvas width  = screen height
        val screenW:     Int,   // landscape screen width
        val screenH:     Int    // landscape screen height
    )

    @Volatile private var cachedState: CachedState? = null

    // ── Clock updater ────────────────────────────────────────────────────────

    private val clockExecutor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "bus-clock").also { it.isDaemon = true }
    }
    @Volatile private var clockFuture: ScheduledFuture<*>? = null

    // ── Public entry point ───────────────────────────────────────────────────

    fun render(request: BusTextRequest) {
        try {
            val screenType = AppConfigUtils.screenType
            val screenW = if (screenType.width  > 0) screenType.width  else DEFAULT_W
            val screenH = if (screenType.height > 0) screenType.height else DEFAULT_H
            // Portrait canvas: width = screenH (swapped for -90° rotation), height = screenW
            val canvasW = screenH
            val canvasH = screenW
            val unitH   = canvasH / TOTAL_ROWS

            val displayLines = (request.lines ?: emptyList()).take(MAX_LINES)
            val stationName  = request.stationName?.trim() ?: ""
            val labels   = List(MAX_LINES) { i -> displayLines.getOrNull(i).label() }
            val arrivals = List(MAX_LINES) { i ->
                displayLines.getOrNull(i)?.arrivalTime?.trim() ?: ""
            }

            val imgDir   = AppConstant.getImgDir()
            val isColors = ScreenType.isColorsScreen(screenType)
            val prev     = cachedState

            if (prev == null || prev.unitH != unitH || prev.screenW != screenW) {
                // ── Full GC16 refresh ────────────────────────────────────────
                val currentTime = NtpClient.formatHHmm()
                LogWriter.i("BusTextRenderUtils: full render — station='$stationName' time=$currentTime")
                val file = generateFullImage(
                    stationName, currentTime, labels, arrivals, canvasW, canvasH, imgDir)
                enqueueImage(file, 0, 0, screenW, screenH, if (isColors) 0 else 2)
                cachedState = CachedState(stationName, labels, arrivals, unitH, canvasW, screenW, screenH)
                scheduleClockUpdates(unitH, canvasW)
            } else {
                // ── Count changed rows first ─────────────────────────────────
                val duMode = if (isColors) 0 else 1
                var changes = 0
                if (stationName != prev.stationName) changes++
                for (i in 0 until MAX_LINES) {
                    if (labels[i] != prev.labels[i] || arrivals[i] != prev.arrivals[i]) changes++
                }

                if (changes > 4) {
                    // ── Full GC16 refresh when many rows change ───────────────
                    val currentTime = NtpClient.formatHHmm()
                    LogWriter.i("BusTextRenderUtils: full refresh ($changes rows changed) — station='$stationName' time=$currentTime")
                    val file = generateFullImage(
                        stationName, currentTime, labels, arrivals, canvasW, canvasH, imgDir)
                    enqueueImage(file, 0, 0, screenW, screenH, if (isColors) 0 else 2)
                    cachedState = CachedState(stationName, labels, arrivals, unitH, canvasW, screenW, screenH)
                    scheduleClockUpdates(unitH, canvasW)
                } else {
                    // ── Selective DU partial refresh ─────────────────────────
                    if (stationName != prev.stationName) {
                        enqueuePartialRow(0, stationName, NtpClient.formatHHmm(),
                            unitH, canvasW, imgDir, duMode)
                    }
                    for (i in 0 until MAX_LINES) {
                        if (labels[i] != prev.labels[i] || arrivals[i] != prev.arrivals[i]) {
                            enqueuePartialRow(i + 1, labels[i], arrivals[i],
                                unitH, canvasW, imgDir, duMode)
                        }
                    }
                    LogWriter.i("BusTextRenderUtils: partial refresh — $changes row(s) changed")
                    cachedState = prev.copy(
                        stationName = stationName, labels = labels, arrivals = arrivals)
                }
            }

            pruneOldFiles(imgDir)
        } catch (e: Exception) {
            LogWriter.e("BusTextRenderUtils render error", e)
        }
    }

    // ── Clock updater ────────────────────────────────────────────────────────

    private fun scheduleClockUpdates(unitH: Int, canvasW: Int) {
        clockFuture?.cancel(false)
        val msToNextMinute = 60_000L - (NtpClient.currentTimeMs() % 60_000L)
        // scheduleWithFixedDelay is safe on Android (no burst catch-up after process uncache)
        clockFuture = clockExecutor.scheduleWithFixedDelay(
            { refreshClock(unitH, canvasW) },
            msToNextMinute, 60_000L, TimeUnit.MILLISECONDS
        )
    }

    private fun refreshClock(unitH: Int, canvasW: Int) {
        val state = cachedState ?: return
        val currentTime = NtpClient.formatHHmm()
        LogWriter.i("BusTextRenderUtils: clock tick — $currentTime")
        enqueuePartialRow(0, state.stationName, currentTime,
            unitH, canvasW, AppConstant.getImgDir(), 1)
    }

    // ── Image generation ─────────────────────────────────────────────────────

    private fun generateFullImage(
        stationName: String,
        currentTime: String,
        labels:      List<String>,
        arrivals:    List<String>,
        canvasW:     Int,
        canvasH:     Int,
        imgDir:      String
    ): File {
        val unitH       = canvasH / TOTAL_ROWS
        val bitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint  = makePaint(unitH)
        val hPad   = (canvasW * 0.04f).toInt()
        val maxFW  = (canvasW * 0.44f).toInt()

        // -90° rotation inverts portrait-y → landscape-x: portrait y=0 becomes the physical TOP.
        // Draw rows top-to-bottom in portrait order; rotation handles the mapping automatically.
        val footerStart = unitH * (MAX_LINES + 1)
        drawRow(canvas, paint, 0, 0, unitH, canvasW, hPad, maxFW, stationName, currentTime)
        for (i in 0 until MAX_LINES) {
            drawRow(canvas, paint, i + 1, (i + 1) * unitH, unitH,
                canvasW, hPad, maxFW, labels[i], arrivals[i])
        }
        paint.color = bgColor(6)
        canvas.drawRect(0f, footerStart.toFloat(), canvasW.toFloat(),
            (footerStart + unitH).toFloat(), paint)
        paint.color = bgColor(7)
        canvas.drawRect(0f, (footerStart + unitH).toFloat(),
            canvasW.toFloat(), canvasH.toFloat(), paint)

        return rotateSave(bitmap, FILE_PREFIX + "${System.currentTimeMillis()}.png", imgDir)
    }

    private fun generateRowImage(
        rowIdx:    Int,
        leftText:  String,
        rightText: String,
        unitH:     Int,
        canvasW:   Int,
        imgDir:    String
    ): File {
        val bitmap = Bitmap.createBitmap(canvasW, unitH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint  = makePaint(unitH)
        val hPad   = (canvasW * 0.04f).toInt()
        val maxFW  = (canvasW * 0.44f).toInt()
        drawRow(canvas, paint, rowIdx, 0, unitH, canvasW, hPad, maxFW, leftText, rightText)
        return rotateSave(bitmap,
            FILE_PREFIX + "r${rowIdx}_${System.currentTimeMillis()}.png", imgDir)
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private fun makePaint(unitH: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = unitH * (3f / 3.5f)
    }

    private fun drawRow(
        canvas: Canvas, paint: Paint,
        rowIdx: Int, top: Int, h: Int, w: Int,
        hPad: Int, maxFW: Int,
        leftText: String, rightText: String
    ) {
        paint.color = bgColor(rowIdx)
        canvas.drawRect(0f, top.toFloat(), w.toFloat(), (top + h).toFloat(), paint)

        val fm       = paint.fontMetrics
        val baseline = top + (h - (fm.descent - fm.ascent)) / 2 - fm.ascent

        paint.color = fgColor(rowIdx)
        if (leftText.isNotEmpty()) {
            val t = truncate(paint, leftText, maxFW)
            val x = if (rowIdx == 0) (w - paint.measureText(t)) / 2f else hPad.toFloat()
            canvas.drawText(t, x, baseline, paint)
        }
        if (rightText.isNotEmpty()) {
            val t = truncate(paint, rightText, maxFW)
            canvas.drawText(t, w - hPad - paint.measureText(t), baseline, paint)
        }
    }

    private fun bgColor(row: Int) = if (row % 2 == 0) Color.BLACK else Color.WHITE
    private fun fgColor(row: Int) = if (row % 2 == 0) Color.WHITE else Color.BLACK

    private fun rotateSave(bitmap: Bitmap, filename: String, imgDir: String): File {
        val matrix  = android.graphics.Matrix().also { it.postRotate(-90f) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val file    = File(imgDir, filename)
        FileOutputStream(file).use { rotated.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        rotated.recycle()
        return file
    }

    private fun truncate(paint: Paint, text: String, maxPx: Int): String {
        if (paint.measureText(text) <= maxPx) return text
        val ellipsis = "…"
        var r = text
        while (r.isNotEmpty() && paint.measureText(r + ellipsis) > maxPx) r = r.dropLast(1)
        return r + ellipsis
    }

    // ── Queue helpers ─────────────────────────────────────────────────────────

    /**
     * Enqueue a partial single-row refresh.
     * After -90° rotation the row strip dimensions are (unitH × canvasW).
     * Its landscape x-offset is rowIdx * unitH (portrait top maps to landscape left).
     */
    private fun enqueuePartialRow(
        rowIdx:      Int,
        leftText:    String,
        rightText:   String,
        unitH:       Int,
        canvasW:     Int,
        imgDir:      String,
        displayMode: Int
    ) {
        val file = generateRowImage(rowIdx, leftText, rightText, unitH, canvasW, imgDir)
        // Mirror the reversed portrait-y order: row 0 header maps to the highest landscape x
        enqueueImage(file, startX = (TOTAL_ROWS - 1 - rowIdx) * unitH, startY = 0,
            width = unitH, height = canvasW, displayMode = displayMode)
    }

    private fun enqueueImage(
        file:  File,
        startX: Int, startY: Int,
        width:  Int, height: Int,
        displayMode: Int
    ) {
        if (!file.exists() || !file.canRead()) {
            LogWriter.e("BusTextRenderUtils: image missing: ${file.absolutePath}")
            return
        }
        val epdImage = EpdImage(file, 0f).apply {
            name             = file.name
            this.startX      = startX
            this.startY      = startY
            this.width       = width
            this.height      = height
            this.displayMode = displayMode
            this.formatType  = EpdImage.FORMAT_TYPE_FILE
        }
        T1000HelperFactory.instance?.run {
            val offered = addImage(epdImage)
            if (!offered) { clearImage(); addImage(epdImage) }
            requestSleepInterrupted()
        }
    }

    // ── File pruning ──────────────────────────────────────────────────────────

    private fun pruneOldFiles(imgDir: String) {
        try {
            val files = File(imgDir)
                .listFiles { _, name -> name.startsWith(FILE_PREFIX) && name.endsWith(".png") }
                ?.sortedByDescending { it.lastModified() } ?: return
            if (files.size <= MAX_CACHED) return
            files.drop(MAX_CACHED).forEach { it.delete() }
        } catch (e: Exception) {
            LogWriter.w("BusTextRenderUtils: prune error: ${e.message}")
        }
    }

    // ── Extension ────────────────────────────────────────────────────────────

    private fun BusLine?.label(): String =
        this?.destinationStopName?.trim()?.takeIf { it.isNotEmpty() }
            ?: this?.busNumber?.trim()?.takeIf { it.isNotEmpty() } ?: ""
}
