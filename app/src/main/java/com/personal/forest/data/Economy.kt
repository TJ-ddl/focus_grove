package com.personal.forest.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Economy {
    /** Successful session reward: 2 coins per minute, +20 for sessions of an hour or more. */
    fun coins(minutes: Int): Int = minutes * 2 + if (minutes >= 60) 20 else 0
    fun witheredCoins(): Int = 0

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
