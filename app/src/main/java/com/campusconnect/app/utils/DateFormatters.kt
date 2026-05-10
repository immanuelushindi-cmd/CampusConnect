package com.campusconnect.app.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Centralised date formatters using java.time (available from API 26 = your minSdk).
 *
 * Why java.time instead of SimpleDateFormat?
 *  - DateTimeFormatter is immutable and thread-safe — safe to share as singletons.
 *  - SimpleDateFormat is NOT thread-safe and was being re-created on every
 *    recomposition inside individual cards, which is wasteful.
 *
 * FIX 10: The original file had mojibake encoding (â€¢) in its format strings,
 * caused by the file being saved with the wrong encoding. The bullet character
 * was displaying as garbled text at runtime. Fixed by ensuring the file is
 * UTF-8 and using the literal • character directly.
 *
 * Usage:
 *   val text = DateFormatters.noticeCard(notice.timestampMs)
 */
object DateFormatters {

    private val zone = ZoneId.systemDefault()

    // FIX 10: Literal • character (U+2022) — was previously mojibake "â€¢"
    private val noticeCardFmt = DateTimeFormatter
        .ofPattern("dd MMM • HH:mm", Locale.getDefault())

    private val dashboardItemFmt = DateTimeFormatter
        .ofPattern("HH:mm", Locale.getDefault())

    // FIX 10: Same fix applied here
    private val noticeDetailFmt = DateTimeFormatter
        .ofPattern("EEEE, dd MMM yyyy • HH:mm", Locale.getDefault())

    private val eventDateFmt = DateTimeFormatter
        .ofPattern("EEE, dd MMM", Locale.getDefault())

    private val eventTimeFmt = DateTimeFormatter
        .ofPattern("HH:mm", Locale.getDefault())

    private val eventDayFmt = DateTimeFormatter
        .ofPattern("dd", Locale.getDefault())

    private val eventMonthFmt = DateTimeFormatter
        .ofPattern("MMM", Locale.getDefault())

    private val eventGroupFmt = DateTimeFormatter
        .ofPattern("MMMM yyyy", Locale.getDefault())

    // ── Public helpers ────────────────────────────────────────────────────────

    fun noticeCard(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(noticeCardFmt)

    fun dashboardItem(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(dashboardItemFmt)

    fun noticeDetail(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(noticeDetailFmt)

    fun eventDate(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(eventDateFmt)

    fun eventTime(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(eventTimeFmt)

    fun eventDay(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(eventDayFmt)

    fun eventMonth(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(eventMonthFmt)

    fun eventGroup(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(eventGroupFmt)
}
