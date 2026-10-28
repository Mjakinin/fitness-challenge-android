package com.challenge.fitnessnag

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

class ChallengeWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_ADD_FITNESS = "com.challenge.fitnessnag.WIDGET_ADD_FITNESS"
        const val ACTION_WIDGET_ADD_JOGGEN = "com.challenge.fitnessnag.WIDGET_ADD_JOGGEN"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ChallengeWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (widgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val challengeManager = ChallengeManager(context)
            val status = challengeManager.getWeekStatus()

            val views = RemoteViews(context.packageName, R.layout.widget_challenge)

            // Score & Text
            views.setTextViewText(R.id.tvWidgetScore, "${status.totalCount} / 4")
            views.setTextViewText(
                R.id.tvWidgetDeadline,
                if (status.isCompleted) "🎉 Ziel erreicht!" else status.deadlineText
            )

            // Open app on click
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetContainer, openAppPendingIntent)

            // Add Fitness Button Intent
            val addFitnessIntent = Intent(context, ChallengeWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_ADD_FITNESS
            }
            val fitnessPendingIntent = PendingIntent.getBroadcast(
                context,
                11,
                addFitnessIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetAddFitness, fitnessPendingIntent)

            // Add Joggen Button Intent
            val addJoggenIntent = Intent(context, ChallengeWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_ADD_JOGGEN
            }
            val joggenPendingIntent = PendingIntent.getBroadcast(
                context,
                12,
                addJoggenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnWidgetAddJoggen, joggenPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = ChallengeManager(context)

        when (intent.action) {
            ACTION_WIDGET_ADD_FITNESS -> {
                val ok = manager.addSession("fitness")
                if (ok) {
                    Toast.makeText(context, "🏋️ +1 Fitness verbucht!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "⚠️ Tageslimit erreicht (Max 2 am Tag)!", Toast.LENGTH_SHORT).show()
                }
                updateAllWidgets(context)
                manager.scheduleNextAlarm()
            }
            ACTION_WIDGET_ADD_JOGGEN -> {
                val ok = manager.addSession("joggen")
                if (ok) {
                    Toast.makeText(context, "🏃 +1 Joggen verbucht!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "⚠️ Tageslimit erreicht (Max 2 am Tag)!", Toast.LENGTH_SHORT).show()
                }
                updateAllWidgets(context)
                manager.scheduleNextAlarm()
            }
        }
    }
}
