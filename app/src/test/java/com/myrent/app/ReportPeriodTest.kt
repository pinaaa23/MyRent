package com.myrent.app

import com.myrent.app.ui.report.PeriodType
import com.myrent.app.ui.report.ReportPeriodHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Unit Test sederhana untuk pengujian batas periode, tahun kabisat, dan pergantian bulan/minggu
 */
class ReportPeriodTest {

    private val timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    private val locale = Locale("id", "ID")

    @Test
    fun testMingguStartOnMonday() {
        val cal = Calendar.getInstance(timeZone, locale).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(2026, Calendar.NOVEMBER, 11) // Rabu, 11 Nov 2026
        }

        val range = ReportPeriodHelper.calculatePeriodRange(PeriodType.MINGGU, 0, cal.timeInMillis)

        val calStart = Calendar.getInstance(timeZone, locale).apply { timeInMillis = range.startMs }
        val calEnd = Calendar.getInstance(timeZone, locale).apply { timeInMillis = range.endMs }

        assertEquals(Calendar.MONDAY, calStart.get(Calendar.DAY_OF_WEEK))
        assertEquals(Calendar.SUNDAY, calEnd.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, calStart.get(Calendar.DAY_OF_MONTH)) // Senin 9 Nov 2026
        assertEquals(15, calEnd.get(Calendar.DAY_OF_MONTH)) // Minggu 15 Nov 2026
    }

    @Test
    fun testLeapYearFebruary() {
        // Tahun Kabisat 2024 (Februari 29 hari)
        val cal2024 = Calendar.getInstance(timeZone, locale).apply {
            set(2024, Calendar.FEBRUARY, 15)
        }
        val range2024 = ReportPeriodHelper.calculatePeriodRange(PeriodType.BULAN, 0, cal2024.timeInMillis)
        val calEnd2024 = Calendar.getInstance(timeZone, locale).apply { timeInMillis = range2024.endMs }
        assertEquals(29, calEnd2024.get(Calendar.DAY_OF_MONTH))

        // Tahun Biasa 2025 (Februari 28 hari)
        val cal2025 = Calendar.getInstance(timeZone, locale).apply {
            set(2025, Calendar.FEBRUARY, 15)
        }
        val range2025 = ReportPeriodHelper.calculatePeriodRange(PeriodType.BULAN, 0, cal2025.timeInMillis)
        val calEnd2025 = Calendar.getInstance(timeZone, locale).apply { timeInMillis = range2025.endMs }
        assertEquals(28, calEnd2025.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testMonthWeekBarsCount() {
        val cal = Calendar.getInstance(timeZone, locale).apply {
            set(2026, Calendar.NOVEMBER, 1)
        }
        val range = ReportPeriodHelper.calculatePeriodRange(PeriodType.BULAN, 0, cal.timeInMillis)
        val bars = ReportPeriodHelper.generateChartBars(PeriodType.BULAN, range)

        // Bulan November (30 hari) harus menghasilkan 5 batang minggu
        assertEquals(5, bars.size)
        assertEquals("1-7", bars[0].label)
        assertEquals("29-30", bars[4].label)
    }

    @Test
    fun testYearBarsCount() {
        val cal = Calendar.getInstance(timeZone, locale).apply {
            set(2026, Calendar.NOVEMBER, 1)
        }
        val range = ReportPeriodHelper.calculatePeriodRange(PeriodType.TAHUN, 0, cal.timeInMillis)
        val bars = ReportPeriodHelper.generateChartBars(PeriodType.TAHUN, range)

        assertEquals(12, bars.size)
        assertEquals("2026", range.displayText)
    }
}
