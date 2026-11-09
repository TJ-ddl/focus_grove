package com.personal.forest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.personal.forest.data.SpeciesRepo
import com.personal.forest.data.Store
import com.personal.forest.session.Focus
import com.personal.forest.session.FocusService
import com.personal.forest.ui.ForestAppRoot
import com.personal.forest.ui.theme.ForestTheme
import com.personal.forest.widget.TodayWidgetProvider

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeQuickStart(intent)
        setContent {
            ForestTheme {
                ForestAppRoot()
            }
        }
        requestNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeQuickStart(intent)
    }

    private fun maybeQuickStart(intent: Intent?) {
        if (intent?.getBooleanExtra(TodayWidgetProvider.EXTRA_QUICK_START, false) != true) return
        intent.removeExtra(TodayWidgetProvider.EXTRA_QUICK_START)
        if (Focus.running.value != null) return
        val store = Store.get(this)
        val settings = store.data.value.settings
        val species = SpeciesRepo.byId(this, settings.lastSpeciesId) ?: return
        if (species.id !in store.data.value.unlocked) return
        FocusService.start(this, species, settings.lastMinutes, "")
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
