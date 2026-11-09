package com.personal.forest.util

import java.time.LocalDate
import java.time.LocalTime

fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

fun formatMinutes(minutes: Int): String =
    if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0) "$h 小时" else "$h 小时 $m 分钟"
    } else "$minutes 分钟"

private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

fun weekdayCn(day: LocalDate): String = weekdays[day.dayOfWeek.value - 1]

fun dayLabel(day: LocalDate): String = "${day.monthValue}月${day.dayOfMonth}日 · ${weekdayCn(day)}"

fun timeLabel(ms: Long): String =
    java.time.Instant.ofEpochMilli(ms)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .let { LocalTime.of(it.hour, it.minute).toString() }
