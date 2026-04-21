package com.xingtai.epd.device.demo.util

import com.sjl.util.LogWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Minimal NTP client.  Queries pool.ntp.org every 30 minutes and stores the
 * offset from the device clock so every call to [currentTimeMs] / [formatHHmm]
 * returns accurate network time even if the system clock is wrong.
 */
object NtpClient {

    private const val HOST = "pool.ntp.org"
    private const val PORT = 123
    private const val TIMEOUT_MS = 5_000
    // Seconds between NTP epoch (1900-01-01) and Unix epoch (1970-01-01)
    private const val EPOCH_DIFF_S = 2_208_988_800L

    @Volatile private var offsetMs = 0L

    private val syncExecutor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "ntp-sync").also { it.isDaemon = true }
    }

    /** Call once at application start. Syncs immediately then every 30 minutes. */
    fun start() {
        syncExecutor.scheduleAtFixedRate(::syncNow, 0L, 30L, TimeUnit.MINUTES)
    }

    private fun syncNow() {
        try {
            val addr = InetAddress.getByName(HOST)
            val buf = ByteArray(48).also { it[0] = 0x1B.toByte() }  // LI=0, VN=3, Mode=3 (client)
            val sock = DatagramSocket()
            sock.soTimeout = TIMEOUT_MS
            sock.send(DatagramPacket(buf, buf.size, addr, PORT))
            sock.receive(DatagramPacket(buf, buf.size))
            sock.close()

            val secs = buf.uint32(40) - EPOCH_DIFF_S
            val frac = buf.uint32(44)
            val ntpMs = secs * 1_000L + (frac * 1_000L ushr 32)
            offsetMs = ntpMs - System.currentTimeMillis()
            LogWriter.i("NtpClient: synced, offset=${offsetMs}ms")
        } catch (e: Exception) {
            LogWriter.w("NtpClient: sync failed – ${e.message}")
        }
    }

    /** Current time in milliseconds, adjusted by the last known NTP offset. */
    fun currentTimeMs(): Long = System.currentTimeMillis() + offsetMs

    /** Current time formatted as HH:mm using NTP-adjusted clock. */
    fun formatHHmm(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTimeMs()))

    private fun ByteArray.uint32(off: Int): Long =
        ((this[off].toLong() and 0xFF) shl 24) or
        ((this[off + 1].toLong() and 0xFF) shl 16) or
        ((this[off + 2].toLong() and 0xFF) shl 8) or
        (this[off + 3].toLong() and 0xFF)
}
