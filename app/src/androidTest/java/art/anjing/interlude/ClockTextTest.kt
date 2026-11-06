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
}
