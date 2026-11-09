package com.personal.forest.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.personal.forest.MainActivity
import com.personal.forest.R
import com.personal.forest.data.Economy
import com.personal.forest.data.Store
import java.time.LocalDate

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val views = build(context)
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
    }

    companion object {
        const val EXTRA_QUICK_START = "quick_start"

        fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun build(context: Context): RemoteViews {
            val data = Store.get(context).data.value
            val today = LocalDate.now()
            val minutes = Economy.minutesOn(data.sessions, today)
            val trees = Economy.treesOn(data.sessions, today)
            val streak = Economy.streak(data.sessions, today)

            val views = RemoteViews(context.packageName, R.layout.widget_today)
            views.setTextViewText(R.id.widget_minutes, "$minutes 分钟")
            views.setTextViewText(R.id.widget_sub, "$trees 棵树 · 连续 $streak 天")
            views.setTextViewText(R.id.widget_quick, "快速开始 ${data.settings.lastMinutes} 分钟")

            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val quick = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(EXTRA_QUICK_START, true)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_title, open)
            views.setOnClickPendingIntent(R.id.widget_minutes, open)
            views.setOnClickPendingIntent(R.id.widget_sub, open)
            views.setOnClickPendingIntent(R.id.widget_quick, quick)
            return views
        }
    }
}
