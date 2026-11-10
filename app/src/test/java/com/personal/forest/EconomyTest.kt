package com.personal.forest

import com.personal.forest.data.Achievements
import com.personal.forest.data.Economy
import com.personal.forest.data.GameData
import com.personal.forest.data.Session
import com.personal.forest.data.Settings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EconomyTest {

    private fun session(
        id: String,
        day: LocalDate,
        minutes: Int = 25,
        status: String = "success",
        tag: String = "",
    ): Session = Session(
        id = id,
        speciesId = "oak",
        startAt = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        plannedMin = minutes,
        actualSec = minutes * 60,
        status = status,
        tag = tag,
        coins = if (status == "success") Economy.coins(minutes) else 0,
    )

    @Test
    fun coinReward() {
        assertEquals(50, Economy.coins(25))
        assertEquals(140, Economy.coins(60))
        assertEquals(240, Economy.coins(110))
    }

    @Test
    fun clearedWitheredTreeStaysInFailureStats() {
        val today = LocalDate.of(2026, 11, 8)
        val sessions = listOf(
            session("1", today, 25),
            session("2", today, 50, status = "withered").copy(cleared = true),
        )
        assertEquals(25, Economy.minutesOn(sessions, today))
        assertEquals(1, Economy.treesOn(sessions, today))
        assertEquals(0.5f, Economy.successRate(sessions), 0.001f)
    }

    @Test
    fun streakCountsConsecutiveDays() {
        val today = LocalDate.of(2026, 11, 8)
        val sessions = listOf(
            session("1", today),
            session("2", today.minusDays(1)),
            session("3", today.minusDays(2)),
            session("4", today.minusDays(4)),
            session("5", today.minusDays(1), status = "withered"),
        )
        assertEquals(3, Economy.streak(sessions, today))
    }

    @Test
    fun streakSurvivesAnEmptyToday() {
        val today = LocalDate.of(2026, 11, 8)
        val sessions = listOf(
            session("1", today.minusDays(1)),
            session("2", today.minusDays(2)),
        )
        assertEquals(2, Economy.streak(sessions, today))
    }

    @Test
    fun witheredTreesDoNotCount() {
        val today = LocalDate.of(2026, 11, 8)
        val sessions = listOf(
            session("1", today, 25),
            session("2", today, 50, status = "withered"),
        )
        assertEquals(25, Economy.minutesOn(sessions, today))
        assertEquals(1, Economy.treesOn(sessions, today))
        assertEquals(0.5f, Economy.successRate(sessions), 0.001f)
    }

    @Test
    fun weeklyHasSevenDaysEndingToday() {
        val today = LocalDate.of(2026, 11, 8)
        val sessions = listOf(
            session("1", today, 30),
            session("2", today.minusDays(6), 45),
        )
        val weekly = Economy.weeklyMinutes(sessions, today)
        assertEquals(7, weekly.size)
        assertEquals(45, weekly.first())
        assertEquals(30, weekly.last())
        assertEquals(0, weekly[3])
    }

    @Test
    fun gameDataJsonRoundTrip() {
        val data = GameData(
            coins = 123,
            sessions = listOf(session("1", LocalDate.of(2026, 11, 8))),
            settings = Settings(strictMode = true, lastSpeciesId = "sakura", lastMinutes = 45),
        )
        val json = Json { encodeDefaults = true }
        val restored = json.decodeFromString<GameData>(json.encodeToString(data))
        assertEquals(data, restored)
    }

    @Test
    fun achievementsUnlockFromASingleHourSession() {
        val today = LocalDate.of(2026, 11, 8)
        val data = GameData(
            sessions = listOf(session("1", today, 65)),
            unlocked = listOf("sapling", "oak"),
        )
        val earned = Achievements.earned(data)
        assertTrue("first_tree" in earned)
        assertTrue("hour" in earned)
        assertFalse("streak3" in earned)
        assertFalse("species5" in earned)
    }
}
