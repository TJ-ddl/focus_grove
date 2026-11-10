package com.personal.forest.data

import android.content.Context
import android.net.Uri
import com.personal.forest.widget.TodayWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class Store private constructor(private val context: Context) {
    private val dir: File = context.filesDir
    private val file = File(dir, "data.json")
    private val bak = File(dir, "data.bak.json")
    private val lock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<GameData> = _data.asStateFlow()

    private fun load(): GameData {
        read(file)?.let { return it }
        read(bak)?.let {
            runCatching { file.writeText(json.encodeToString(it)) }
            return it
        }
        return GameData()
    }

    private fun read(f: File): GameData? = runCatching {
        if (f.exists()) json.decodeFromString<GameData>(f.readText()) else null
    }.getOrNull()

    private fun save(d: GameData) {
        runCatching {
            val tmp = File(dir, "data.tmp")
            tmp.writeText(json.encodeToString(d))
            if (file.exists()) file.copyTo(bak, overwrite = true)
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }

    fun update(block: (GameData) -> GameData) {
        synchronized(lock) {
            val next = block(_data.value)
            _data.value = next
            save(next)
        }
        TodayWidgetProvider.update(context)
    }

    /** Adds a finished session and returns ids of achievements unlocked by it. */
    fun addSession(session: Session): List<String> {
        var newly: List<String> = emptyList()
        update { current ->
            if (current.sessions.any { it.id == session.id }) return@update current
            val next = current.copy(
                coins = current.coins + session.coins,
                sessions = current.sessions + session,
            )
            val earned = Achievements.earned(next)
            newly = earned.filterNot { it in next.achievements }
            next.copy(achievements = (next.achievements + newly).distinct())
        }
        return newly
    }

    fun unlock(species: Species): Boolean {
        var ok = false
        update { current ->
            if (species.id in current.unlocked || current.coins < species.price) current
            else {
                ok = true
                current.copy(
                    coins = current.coins - species.price,
                    unlocked = current.unlocked + species.id,
                )
            }
        }
        return ok
    }

    /** Hides a withered tree from the forest without deleting its failed session. */
    fun clearWithered(sessionId: String, cost: Int = Economy.CLEAR_WITHERED_COST): Boolean {
        var ok = false
        update { current ->
            val target = current.sessions.firstOrNull { it.id == sessionId }
            if (target == null || target.status != "withered" || target.cleared ||
                current.coins < cost
            ) current
            else {
                ok = true
                current.copy(
                    coins = current.coins - cost,
                    sessions = current.sessions.map {
                        if (it.id == sessionId) it.copy(cleared = true) else it
                    },
                )
            }
        }
        return ok
    }

    fun setSettings(transform: (Settings) -> Settings) {
        update { it.copy(settings = transform(it.settings)) }
    }

    fun export(uri: Uri): Boolean = try {
        val text = json.encodeToString(_data.value)
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
    } catch (e: Exception) {
        false
    }

    fun import(uri: Uri): Boolean = try {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().decodeToString()
        } ?: return false
        val parsed = json.decodeFromString<GameData>(text)
        val clean = parsed.copy(
            coins = parsed.coins.coerceAtLeast(0),
            unlocked = parsed.unlocked.ifEmpty { listOf("sapling", "oak") },
            sessions = parsed.sessions.takeLast(20000),
        )
        synchronized(lock) {
            _data.value = clean
            save(clean)
        }
        TodayWidgetProvider.update(context)
        true
    } catch (e: Exception) {
        false
    }

    companion object {
        @Volatile
        private var instance: Store? = null

        fun get(context: Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(context.applicationContext).also { instance = it }
            }
    }
}
