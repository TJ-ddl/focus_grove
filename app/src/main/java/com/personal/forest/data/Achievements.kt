package com.personal.forest.data

data class Achievement(val id: String, val name: String, val desc: String)

object Achievements {
    val all = listOf(
        Achievement("first_tree", "第一棵树", "完成第一次专注"),
        Achievement("hour", "一小时", "单次专注满 60 分钟"),
        Achievement("day2h", "两小时的一天", "单日专注满 120 分钟"),
        Achievement("streak3", "三日成林", "连续 3 天专注"),
        Achievement("streak7", "一周之约", "连续 7 天专注"),
        Achievement("total10h", "十小时", "累计专注满 10 小时"),
        Achievement("trees10", "十棵树", "累计种下 10 棵成功的树"),
        Achievement("species5", "收藏家", "解锁 5 个树种"),
    )

    private val byId = all.associateBy { it.id }

    fun name(id: String): String = byId[id]?.name ?: id

    /** All achievements satisfied by the current data. */
    fun earned(data: GameData): List<String> {
        val success = data.sessions.filter { it.status == "success" }
        val earned = mutableListOf<String>()

        if (success.isNotEmpty()) earned += "first_tree"
        if (success.any { it.plannedMin >= 60 }) earned += "hour"
        if (success.groupBy { Economy.dayOf(it.startAt) }
                .any { (_, list) -> list.sumOf { it.plannedMin } >= 120 }) earned += "day2h"
        val streak = Economy.streak(data.sessions)
        if (streak >= 3) earned += "streak3"
        if (streak >= 7) earned += "streak7"
        if (success.sumOf { it.plannedMin } >= 600) earned += "total10h"
        if (success.size >= 10) earned += "trees10"
        if (data.unlocked.size >= 5) earned += "species5"
        return earned
    }
}
