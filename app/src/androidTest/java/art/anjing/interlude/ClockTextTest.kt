package art.anjing.interlude

import androidx.test.ext.junit.runners.AndroidJUnit4
import art.anjing.interlude.ui.clockText
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockTextTest {
    @Test fun lunarCalendarCoversNewYearAndLeapMonth() {
        val zone = ZoneId.of("Asia/Shanghai")
        assertEquals("丙午年正月初一", clockText(ZonedDateTime.of(2026, 2, 17, 12, 0, 0, 0, zone)).lunar)
        assertEquals("丙午年正月初一", clockText(ZonedDateTime.of(2026, 2, 17, 0, 1, 0, 0, zone)).lunar)
        assertEquals("乙巳年闰六月初一", clockText(ZonedDateTime.of(2025, 7, 25, 12, 0, 0, 0, zone)).lunar)
        assertEquals("乙巳年九月十六", clockText(ZonedDateTime.of(2025, 11, 5, 12, 0, 0, 0, zone)).lunar)
        assertEquals("TUESDAY 17 FEBRUARY", clockText(ZonedDateTime.of(2026, 2, 17, 12, 0, 0, 0, zone)).date.uppercase())
    }

    @Test fun lunarYearChangesAtNewYearRatherThanGregorianNewYear() {
        fun lunar(date: String) = clockText(ZonedDateTime.parse(date + "T12:00:00+08:00[Asia/Shanghai]")).lunar
        assertEquals("癸卯年腊月三十", lunar("2024-02-09"))
        assertEquals("甲辰年正月初一", lunar("2024-02-10"))
        assertEquals("乙巳年冬月十三", lunar("2026-01-01"))
        assertEquals("乙巳年腊月廿九", lunar("2026-02-16"))
        assertEquals("丙午年正月初一", lunar("2026-02-17"))
    }

    @Test fun leapMonthHasItsOwnStartAndEnd() {
        fun lunar(date: String) = clockText(ZonedDateTime.parse(date + "T12:00:00+08:00[Asia/Shanghai]")).lunar
        assertEquals("乙巳年六月三十", lunar("2025-07-24"))
        assertEquals("乙巳年闰六月初一", lunar("2025-07-25"))
        assertEquals("乙巳年闰六月廿九", lunar("2025-08-22"))
        assertEquals("乙巳年七月初一", lunar("2025-08-23"))
    }

    @Test fun sameInstantUsesLocalCalendarDayIncludingFixedOffsets() {
        val instant = java.time.Instant.parse("2026-02-16T16:30:00Z")
        for (zone in listOf("Asia/Shanghai", "+08:00", "Asia/Tokyo")) {
            assertEquals(zone, "丙午年正月初一", clockText(instant.atZone(ZoneId.of(zone))).lunar)
        }
        for (zone in listOf("UTC", "America/Los_Angeles", "-08:00")) {
            assertEquals(zone, "乙巳年腊月廿九", clockText(instant.atZone(ZoneId.of(zone))).lunar)
        }
        val midnight = ZonedDateTime.parse("2026-02-17T00:00:00+08:00[Asia/Shanghai]")
        assertEquals("乙巳年腊月廿九", clockText(midnight.minusNanos(1)).lunar)
        assertEquals("丙午年正月初一", clockText(midnight).lunar)
    }
}
