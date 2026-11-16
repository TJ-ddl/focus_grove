package com.personal.forest.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

object Economy {
    /** Cosmetic removal of a withered tree; the failed session stays in the stats. */
    const val CLEAR_WITHERED_COST = 3000

    /** Successful session reward: 2 coins per minute, +20 for sessions of an hour or more. */
    fun coins(minutes: Int): Int = minutes * 2 + if (minutes >= 60) 20 else 0
    fun witheredCoins(): Int = 0

    /** Stage of a finished tree by focus length: <30 sapling, <90 young, 90+ full-grown. */
    fun stageForMinutes(min: Int): Int = when {
        min < 30 -> 1
        min < 90 -> 2
        else -> 3
    }

    /** Tree slot width as a fraction of the day-forest content width, driven by focus length. */
    fun widthForMinutes(min: Int): Float =
        0.26f + 0.18f * (min.coerceIn(10, 120) / 120f)

    fun dayOf(ms: Long): LocalDate =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    fun successful(sessions: List<Session>): List<Session> =
        sessions.filter { it.status == "success" }

    fun minutesOn(sessions: List<Session>, day: LocalDate): Int =
        successful(sessions).filter { dayOf(it.startAt) == day }.sumOf { it.plannedMin }

    fun treesOn(sessions: List<Session>, day: LocalDate): Int =
        successful(sessions).count { dayOf(it.startAt) == day }

    fun totalMinutes(sessions: List<Session>): Int =
        successful(sessions).sumOf { it.plannedMin }

    /** Consecutive days with at least one successful tree, ending today (or yesterday if today is still empty). */
    fun streak(sessions: List<Session>, today: LocalDate = LocalDate.now()): Int {
        val days = successful(sessions).map { dayOf(it.startAt) }.toSet()
        if (days.isEmpty()) return 0
        var cursor = if (today in days) today else today.minusDays(1)
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /** Minutes for the last 7 days, oldest first, today last. */
    fun weeklyMinutes(sessions: List<Session>, today: LocalDate = LocalDate.now()): List<Int> =
        (6 downTo 0).map { daysAgo -> minutesOn(sessions, today.minusDays(daysAgo.toLong())) }

    /** Trees for the last 7 days, oldest first, today last. */
    fun weeklyTrees(sessions: List<Session>, today: LocalDate = LocalDate.now()): List<Int> =
        (6 downTo 0).map { daysAgo -> treesOn(sessions, today.minusDays(daysAgo.toLong())) }

    fun weeklyAverageMinutes(sessions: List<Session>, today: LocalDate = LocalDate.now()): Int =
        weeklyMinutes(sessions, today).average().roundToInt()

    fun longestSession(sessions: List<Session>): Int =
        successful(sessions).maxOfOrNull { it.plannedMin } ?: 0

    /** Minutes of one tag for the last 7 days, oldest first, today last. */
    fun tagWeeklyMinutes(sessions: List<Session>, tag: String, today: LocalDate = LocalDate.now()): List<Int> =
        (6 downTo 0).map { daysAgo ->
            successful(sessions)
                .filter { it.tag == tag && dayOf(it.startAt) == today.minusDays(daysAgo.toLong()) }
                .sumOf { it.plannedMin }
        }

    /** Successful minutes bucketed by start hour: 5–10 morning, 11–16 afternoon, 17–21 evening, else night. */
    fun daypartMinutes(sessions: List<Session>): List<Pair<String, Int>> {
        val totals = HashMap<String, Int>()
        successful(sessions).forEach { s ->
            val h = Instant.ofEpochMilli(s.startAt).atZone(ZoneId.systemDefault()).hour
            val part = when (h) {
                in 5..10 -> "上午"
                in 11..16 -> "下午"
                in 17..21 -> "晚上"
                else -> "深夜"
            }
            totals[part] = (totals[part] ?: 0) + s.plannedMin
        }
        return listOf("上午", "下午", "晚上", "深夜").mapNotNull { p -> totals[p]?.let { p to it } }
    }

    fun tagMinutes(sessions: List<Session>): List<Pair<String, Int>> =
        successful(sessions)
            .filter { it.tag.isNotBlank() }
            .groupBy { it.tag }
            .map { (tag, list) -> tag to list.sumOf { it.plannedMin } }
            .sortedByDescending { it.second }

    fun successRate(sessions: List<Session>): Float {
        if (sessions.isEmpty()) return 0f
        return successful(sessions).size.toFloat() / sessions.size
    }
}
