package com.personal.forest.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object SpeciesRepo {
    @Volatile
    private var cache: List<Species>? = null

    fun get(context: Context): List<Species> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val list = context.assets.open("species.json").bufferedReader().use { reader ->
                Json.decodeFromString<List<Species>>(reader.readText())
            }
            cache = list
            return list
        }
    }

    fun byId(context: Context, id: String): Species? = get(context).firstOrNull { it.id == id }
}
