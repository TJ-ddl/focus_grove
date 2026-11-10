package com.personal.forest

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.personal.forest.data.Store
import com.personal.forest.session.Focus
import com.personal.forest.session.FocusService
import com.personal.forest.util.foregroundPackage
import com.personal.forest.util.hasUsageAccess

class ForestApp : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        createChannels()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Leaving the app while the screen is on kills the tree.
     * Screen off means the phone is simply put down, which must NOT kill it.
     */
    override fun onStop(owner: LifecycleOwner) {
        if (Focus.running.value == null) return
        // A short delay avoids racing the screen-off transition and quick app switches.
        Handler(Looper.getMainLooper()).postDelayed({
            val pm = getSystemService(PowerManager::class.java)
            val state = ProcessLifecycleOwner.get().lifecycle.currentState
            if (Focus.running.value != null &&
                pm.isInteractive &&
                !state.isAtLeast(Lifecycle.State.STARTED)
            ) {
                val settings = Store.get(this@ForestApp).data.value.settings
                val target = if (settings.whitelist.isNotEmpty() &&
                    hasUsageAccess(this@ForestApp)
                ) {
                    foregroundPackage(this@ForestApp)
                } else {
                    null
                }
                if (target == null || target !in settings.whitelist) {
                    FocusService.instance?.abandonFromApp()
                }
            }
        }, 1200L)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RUNNING, "专注计时", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "完成提醒", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    companion object {
        const val CHANNEL_RUNNING = "focus_running"
        const val CHANNEL_DONE = "focus_done"
    }
}
