package art.anjing.interlude.ui

import android.icu.util.ChineseCalendar
import android.icu.util.Calendar
import android.icu.util.TimeZone
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class ClockText(val time: String, val date: String, val lunar: String)

internal fun clockText(now: ZonedDateTime = ZonedDateTime.now()): ClockText {
    val lunar = ChineseCalendar(TimeZone.getTimeZone(now.zone.id)).apply { timeInMillis = now.toInstant().toEpochMilli() }
    val months = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    val stems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val branches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val day = lunar.get(Calendar.DAY_OF_MONTH)
    val digits = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
    val dayText = when (day) {
        in 1..10 -> "初${digits[day - 1]}"
        in 11..19 -> "十${digits[day - 11]}"
        20 -> "二十"
        in 21..29 -> "廿${digits[day - 21]}"
        else -> "三十"
    }
    // ChineseCalendar.YEAR counts from 1 within the sexagenary cycle.
    val cycleYear = lunar.get(Calendar.YEAR)
    val yearText = "${stems[(cycleYear - 1) % 10]}${branches[(cycleYear - 1) % 12]}年"
    return ClockText(
        now.format(DateTimeFormatter.ofPattern("HH:mm")),
        now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)),
        "$yearText${if (lunar.get(Calendar.IS_LEAP_MONTH) == 1) "闰" else ""}${months[lunar.get(Calendar.MONTH)]}月$dayText",
    )
}
