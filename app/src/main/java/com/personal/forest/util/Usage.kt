package com.personal.forest.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

/** Last app resumed in the past 10 seconds; null when the OEM throttles usage events. */
fun foregroundPackage(context: Context): String? {
    val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
    val now = System.currentTimeMillis()
    val events = usm.queryEvents(now - 10_000L, now) ?: return null
    val event = UsageEvents.Event()
    var pkg: String? = null
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val resumed = if (Build.VERSION.SDK_INT >= 29) {
            event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }
        if (resumed) pkg = event.packageName
    }
    return pkg
}
