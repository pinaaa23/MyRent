package com.myrent.app.ui.report

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class PeriodType {
    HARI, MINGGU, BULAN, TAHUN
}

data class PeriodRange(
    val startMs: Long,
    val endMs: Long,
    val displayText: String,
    val currentButtonText: String,
    val isCurrentPeriod: Boolean
)

data class BarChartItem(
    val label: String,        // Label X-axis (misal "Sen", "1-7", "Jan")
    val fullLabel: String,    // Label Tooltip (misal "Senin, 09 Nov 2026")
    val value: Long,          // Nilai Omzet Kotor dalam rentang ini
    val startMs: Long,
    val endMs: Long
)

/**
 * Class murni helper tanggal & periode untuk Zona Waktu Lokal Perangkat
 */
object ReportPeriodHelper {

    private val locale = Locale("id", "ID")

    fun getCalendar(): Calendar {
        val cal = Calendar.getInstance(TimeZone.getDefault(), locale)
        cal.firstDayOfWeek = Calendar.MONDAY // Minggu dimulai hari Senin
        return cal
    }

    /**
     * Menghitung rentang tanggal (startMs, endMs) dan label teks berdasarkan jenis periode dan offset (-1, 0, +1)
     */
    fun calculatePeriodRange(periodType: PeriodType, offset: Int, nowMs: Long = System.currentTimeMillis()): PeriodRange {
        val cal = getCalendar()
        cal.timeInMillis = nowMs

        when (periodType) {
            PeriodType.HARI -> {
                cal.add(Calendar.DAY_OF_YEAR, offset)
                setStartOfDay(cal)
                val startMs = cal.timeInMillis

                setEndOfDay(cal)
                val endMs = cal.timeInMillis

                val sdfText = SimpleDateFormat("dd MMM yyyy", locale).apply { this.timeZone = TimeZone.getDefault() }
                val displayText = sdfText.format(Date(startMs))

                val isCurrent = isSameDay(startMs, nowMs)
                return PeriodRange(startMs, endMs, displayText, "Hari ini", isCurrent)
            }

            PeriodType.MINGGU -> {
                cal.add(Calendar.WEEK_OF_YEAR, offset)
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                setStartOfDay(cal)
                val startMs = cal.timeInMillis

                cal.add(Calendar.DAY_OF_WEEK, 6)
                setEndOfDay(cal)
                val endMs = cal.timeInMillis

                val sdfDay = SimpleDateFormat("dd MMM", locale).apply { this.timeZone = TimeZone.getDefault() }
                val sdfYear = SimpleDateFormat("yyyy", locale).apply { this.timeZone = TimeZone.getDefault() }
                val displayText = "${sdfDay.format(Date(startMs))} - ${sdfDay.format(Date(endMs))} ${sdfYear.format(Date(endMs))}"

                val isCurrent = nowMs in startMs..endMs
                return PeriodRange(startMs, endMs, displayText, "Minggu ini", isCurrent)
            }

            PeriodType.BULAN -> {
                cal.add(Calendar.MONTH, offset)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                setStartOfDay(cal)
                val startMs = cal.timeInMillis

                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                setEndOfDay(cal)
                val endMs = cal.timeInMillis

                val sdfMonth = SimpleDateFormat("MMMM yyyy", locale).apply { this.timeZone = TimeZone.getDefault() }
                val displayText = sdfMonth.format(Date(startMs))

                val isCurrent = nowMs in startMs..endMs
                return PeriodRange(startMs, endMs, displayText, "Bulan ini", isCurrent)
            }

            PeriodType.TAHUN -> {
                cal.add(Calendar.YEAR, offset)
                cal.set(Calendar.DAY_OF_YEAR, 1)
                setStartOfDay(cal)
                val startMs = cal.timeInMillis

                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
                cal.set(Calendar.DAY_OF_YEAR, maxDay)
                setEndOfDay(cal)
                val endMs = cal.timeInMillis

                val sdfYear = SimpleDateFormat("yyyy", locale).apply { this.timeZone = TimeZone.getDefault() }
                val displayText = sdfYear.format(Date(startMs))

                val isCurrent = nowMs in startMs..endMs
                return PeriodRange(startMs, endMs, displayText, "Tahun ini", isCurrent)
            }
        }
    }

    /**
     * Menghasilkan daftar sub-rentang (batang-batang) untuk Grafik Canvas
     */
    fun generateChartBars(periodType: PeriodType, periodRange: PeriodRange): List<BarChartItem> {
        val bars = mutableListOf<BarChartItem>()
        val cal = getCalendar()

        when (periodType) {
            PeriodType.HARI -> {
                cal.timeInMillis = periodRange.endMs
                setStartOfDay(cal)
                cal.add(Calendar.DAY_OF_YEAR, -6)

                val sdfShort = SimpleDateFormat("EEE", locale).apply { this.timeZone = TimeZone.getDefault() }
                val sdfFull = SimpleDateFormat("EEE, dd MMM yyyy", locale).apply { this.timeZone = TimeZone.getDefault() }

                for (i in 0..6) {
                    setStartOfDay(cal)
                    val sMs = cal.timeInMillis
                    setEndOfDay(cal)
                    val eMs = cal.timeInMillis

                    bars.add(
                        BarChartItem(
                            label = sdfShort.format(Date(sMs)),
                            fullLabel = sdfFull.format(Date(sMs)),
                            value = 0L,
                            startMs = sMs,
                            endMs = eMs
                        )
                    )
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            PeriodType.MINGGU -> {
                cal.timeInMillis = periodRange.startMs
                val sdfShort = SimpleDateFormat("EEE", locale).apply { this.timeZone = TimeZone.getDefault() }
                val sdfFull = SimpleDateFormat("EEEE, dd MMM yyyy", locale).apply { this.timeZone = TimeZone.getDefault() }

                for (i in 0..6) {
                    setStartOfDay(cal)
                    val sMs = cal.timeInMillis
                    setEndOfDay(cal)
                    val eMs = cal.timeInMillis

                    bars.add(
                        BarChartItem(
                            label = sdfShort.format(Date(sMs)),
                            fullLabel = sdfFull.format(Date(sMs)),
                            value = 0L,
                            startMs = sMs,
                            endMs = eMs
                        )
                    )
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            PeriodType.BULAN -> {
                cal.timeInMillis = periodRange.startMs
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val sdfMonthYear = SimpleDateFormat("MMM yyyy", locale).apply { this.timeZone = TimeZone.getDefault() }

                val weekRanges = listOf(
                    1..7,
                    8..14,
                    15..21,
                    22..28,
                    29..maxDays
                )

                weekRanges.forEach { range ->
                    if (range.start <= maxDays) {
                        val endDay = range.endInclusive.coerceAtMost(maxDays)

                        cal.set(Calendar.DAY_OF_MONTH, range.start)
                        setStartOfDay(cal)
                        val sMs = cal.timeInMillis

                        cal.set(Calendar.DAY_OF_MONTH, endDay)
                        setEndOfDay(cal)
                        val eMs = cal.timeInMillis

                        bars.add(
                            BarChartItem(
                                label = "${range.start}-$endDay",
                                fullLabel = "Tgl ${range.start}-$endDay ${sdfMonthYear.format(Date(sMs))}",
                                value = 0L,
                                startMs = sMs,
                                endMs = eMs
                            )
                        )
                    }
                }
            }

            PeriodType.TAHUN -> {
                cal.timeInMillis = periodRange.startMs
                val sdfShort = SimpleDateFormat("MMM", locale).apply { this.timeZone = TimeZone.getDefault() }
                val sdfFull = SimpleDateFormat("MMMM yyyy", locale).apply { this.timeZone = TimeZone.getDefault() }

                for (m in 0..11) {
                    cal.set(Calendar.MONTH, m)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    setStartOfDay(cal)
                    val sMs = cal.timeInMillis

                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, maxDay)
                    setEndOfDay(cal)
                    val eMs = cal.timeInMillis

                    bars.add(
                        BarChartItem(
                            label = sdfShort.format(Date(sMs)),
                            fullLabel = sdfFull.format(Date(sMs)),
                            value = 0L,
                            startMs = sMs,
                            endMs = eMs
                        )
                    )
                }
            }
        }

        return bars
    }

    private fun setStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun setEndOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val cal1 = getCalendar().apply { timeInMillis = ts1 }
        val cal2 = getCalendar().apply { timeInMillis = ts2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
