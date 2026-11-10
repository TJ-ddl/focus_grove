package com.personal.forest.session

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.personal.forest.ForestApp
import com.personal.forest.MainActivity
import com.personal.forest.R
import com.personal.forest.data.Economy
import com.personal.forest.data.Session
import com.personal.forest.data.Species
import com.personal.forest.data.Store
import com.personal.forest.util.foregroundPackage
import com.personal.forest.util.hasUsageAccess
import com.personal.forest.widget.TodayWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.random.Random

class FocusService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var store: Store
    private lateinit var prefs: SharedPreferences
    private lateinit var power: PowerManager
    private var timerJob: Job? = null
    private var strictJob: Job? = null
    private var current: RunningSession? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = Store.get(this)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        power = getSystemService(PowerManager::class.java)
        restore()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (current == null) {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 25).coerceIn(10, 120)
                val r = RunningSession(
                    id = UUID.randomUUID().toString(),
                    speciesId = intent.getStringExtra(EXTRA_SPECIES) ?: "sapling",
                    speciesName = intent.getStringExtra(EXTRA_SPECIES_NAME) ?: "树",
                    plannedMin = minutes,
                    tag = intent.getStringExtra(EXTRA_TAG) ?: "",
                    startWall = System.currentTimeMillis(),
                    startElapsed = SystemClock.elapsedRealtime(),
                )
                begin(r)
            }
            ACTION_ABANDON -> abandon()
        }
        if (current == null && intent?.action == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun restore() {
        val raw = prefs.getString(KEY_RUNNING, null) ?: return
        val r = runCatching { Json.decodeFromString<RunningSession>(raw) }.getOrNull() ?: return
        current = r
        Focus.running.value = r
        startForeground(NOTIF_RUNNING, buildRunningNotification(r, remainingMs(r)))
        startLoop(r)
        startStrictWatch(r)
    }

    private fun begin(r: RunningSession) {
        current = r
        prefs.edit().putString(KEY_RUNNING, Json.encodeToString(r)).apply()
        Focus.running.value = r
        startForeground(NOTIF_RUNNING, buildRunningNotification(r, remainingMs(r)))
        startLoop(r)
        startStrictWatch(r)
        TodayWidgetProvider.update(this)
    }

    private fun totalMs(r: RunningSession): Long = r.plannedMin * 60_000L

    private fun remainingMs(r: RunningSession): Long =
        (totalMs(r) - (SystemClock.elapsedRealtime() - r.startElapsed)).coerceAtLeast(0L)

    private fun startLoop(r: RunningSession) {
        timerJob?.cancel()
        timerJob = scope.launch {
            var lastMinute = -1
            while (isActive) {
                val remaining = remainingMs(r)
                if (remaining <= 0L) {
                    finish(r, success = true)
                    return@launch
                }
                val minute = ((remaining + 59_999L) / 60_000L).toInt()
                if (minute != lastMinute) {
                    lastMinute = minute
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIF_RUNNING, buildRunningNotification(r, remaining))
                }
                delay(1000)
            }
        }
    }

    /**
     * Best-effort watch: poll the foreground app and wither after 6s elsewhere.
     * Runs in strict mode, and also when a whitelist exists so leaving a whitelisted
     * app for another one still counts as leaving.
     * shortcut: usage events can be throttled by the OEM, so this is a guard, not a wall.
     */
    private fun startStrictWatch(r: RunningSession) {
        strictJob?.cancel()
        strictJob = scope.launch {
            var misses = 0
            var lastForeground: String? = null
            while (isActive) {
                delay(3000)
                val settings = store.data.value.settings
                if ((!settings.strictMode && settings.whitelist.isEmpty()) ||
                    !hasUsageAccess(this@FocusService) ||
                    !power.isInteractive
                ) {
                    misses = 0
                    continue
                }
                val fg = foregroundPackage(this@FocusService) ?: lastForeground
                lastForeground = fg
                if (fg == null) continue
                if (fg == packageName || exempt(fg)) misses = 0 else misses++
                if (misses >= 2) {
                    finish(r, success = false)
                    return@launch
                }
            }
        }
    }

    private fun finish(r: RunningSession, success: Boolean) {
        if (current?.id != r.id) return
        current = null
        timerJob?.cancel()
        strictJob?.cancel()

        val actualSec = if (success) {
            r.plannedMin * 60
        } else {
            ((SystemClock.elapsedRealtime() - r.startElapsed) / 1000).toInt()
                .coerceIn(0, r.plannedMin * 60)
        }
        val session = Session(
            id = r.id,
            speciesId = r.speciesId,
            startAt = r.startWall,
            plannedMin = r.plannedMin,
            actualSec = actualSec,
            status = if (success) "success" else "withered",
            tag = r.tag,
            coins = if (success) Economy.coins(r.plannedMin) else 0,
            seed = Random.nextInt(),
        )

        prefs.edit().remove(KEY_RUNNING).apply()
        val newly = store.addSession(session)
        Focus.running.value = null
        Focus.result.value = FinishedResult(session, newly)
        if (success) notifyDone(session)
        TodayWidgetProvider.update(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Called from the process lifecycle observer when the user leaves the app with the screen on. */
    fun abandonFromApp() {
        current?.let { finish(it, success = false) }
    }

    private fun abandon() {
        current?.let { finish(it, success = false) }
    }

    private fun buildRunningNotification(r: RunningSession, remainingMs: Long): Notification {
        val minutes = ((remainingMs + 59_999L) / 60_000L).toInt()
        return NotificationCompat.Builder(this, ForestApp.CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_tree)
            .setContentTitle("正在专注 · ${r.speciesName}")
            .setContentText("还剩 $minutes 分钟，别离开")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun notifyDone(session: Session) {
        val n = NotificationCompat.Builder(this, ForestApp.CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_tree)
            .setContentTitle("树长大了")
            .setContentText("专注 ${session.plannedMin} 分钟，获得 ${session.coins} 金币")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_DONE, n)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun exempt(pkg: String): Boolean =
        pkg in EXEMPT_PACKAGES || pkg in store.data.value.settings.whitelist

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        // If the system destroys the service mid-session, do not leave a zombie
        // session behind for the next start to restore.
        if (current != null) {
            prefs.edit().remove(KEY_RUNNING).apply()
            Focus.running.value = null
            current = null
        }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: FocusService? = null
            private set

        const val ACTION_START = "com.personal.forest.action.START"
        const val ACTION_ABANDON = "com.personal.forest.action.ABANDON"
        private const val EXTRA_SPECIES = "species"
        private const val EXTRA_SPECIES_NAME = "species_name"
        private const val EXTRA_MINUTES = "minutes"
        private const val EXTRA_TAG = "tag"
        private const val PREFS = "focus"
        private const val KEY_RUNNING = "running"
        private const val NOTIF_RUNNING = 1
        private const val NOTIF_DONE = 2

        private val EXEMPT_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.phone",
            "com.android.incallui",
            "com.google.android.dialer",
            "com.android.server.telecom",
        )

        fun start(context: Context, species: Species, minutes: Int, tag: String) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SPECIES, species.id)
                putExtra(EXTRA_SPECIES_NAME, species.name)
                putExtra(EXTRA_MINUTES, minutes)
                putExtra(EXTRA_TAG, tag)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
