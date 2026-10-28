package com.challenge.fitnessnag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class QuickActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val manager = ChallengeManager(context)

        when (intent?.action) {
            "ACTION_LOG_FITNESS" -> {
                val success = manager.addSession("fitness")
                if (success) {
                    Toast.makeText(context, "🏋️ 1x Fitness eingetragen!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "⚠️ Tageslimit von 2 Einheiten erreicht!", Toast.LENGTH_SHORT).show()
                }
            }
            "ACTION_LOG_JOGGEN" -> {
                val success = manager.addSession("joggen")
                if (success) {
                    Toast.makeText(context, "🏃 1x Joggen eingetragen!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "⚠️ Tageslimit von 2 Einheiten erreicht!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Re-display or dismiss notification according to updated progress
        val status = manager.getWeekStatus()
        if (status.isCompleted) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(ChallengeManager.NOTIFICATION_ID)
            Toast.makeText(context, "🎉 WOCHENZIEL ERREICHT! Du bist frei!", Toast.LENGTH_LONG).show()
        } else {
            manager.showNagNotification()
        }
    }
}
