package com.personal.forest.data

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val speciesId: String,
    val startAt: Long,
    val plannedMin: Int,
    val actualSec: Int,
    val status: String, // "success" | "withered"
    val tag: String = "",
    val coins: Int = 0,
    val seed: Int = 0,
)

@Serializable
data class Settings(
    val strictMode: Boolean = false,
    val lastSpeciesId: String = "sapling",
    val lastMinutes: Int = 25,
)

@Serializable
data class GameData(
    val version: Int = 1,
    val coins: Int = 600,
    val unlocked: List<String> = listOf("sapling", "oak"),
    val sessions: List<Session> = emptyList(),
    val achievements: List<String> = emptyList(),
    val settings: Settings = Settings(),
)

@Serializable
data class Species(
    val id: String,
    val name: String,
    val rarity: String, // free | common | rare | legendary
    val price: Int,
    val desc: String,
    val art: String, // drawable prefix, stages are "${art}_1" .. "${art}_3"
)

val TAGS = listOf("学习", "工作", "阅读", "运动", "其他")

fun rarityLabel(rarity: String): String = when (rarity) {
    "free" -> "初始"
    "common" -> "普通"
    "rare" -> "稀有"
    "legendary" -> "传说"
    else -> rarity
}
