package com.challenge.fitnessnag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NagAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val manager = ChallengeManager(context)
        manager.showNagNotification()
        manager.scheduleNextAlarm()
    }
}
