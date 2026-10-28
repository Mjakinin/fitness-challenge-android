package com.challenge.fitnessnag

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChallengeManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fitness_challenge_prefs", Context.MODE_PRIVATE)

    companion object {
        const val CHANNEL_ID = "fitness_nag_channel_high"
        const val NOTIFICATION_ID = 4040
        const val ALARM_REQUEST_CODE = 999
        val DAY_NAMES = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        val FULL_DAY_NAMES = listOf(
            "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"
        )
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun getAllSessions(): List<WorkoutSession> {
        val jsonStr = prefs.getString("sessions_json", "[]") ?: "[]"
        val list = mutableListOf<WorkoutSession>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WorkoutSession(
                        id = obj.getString("id"),
                        date = obj.getString("date"),
                        time = obj.getString("time"),
                        sport = obj.getString("sport")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveSessions(sessions: List<WorkoutSession>) {
        val arr = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("date", s.date)
            obj.put("time", s.time)
            obj.put("sport", s.sport)
            arr.put(obj)
        }
        prefs.edit().putString("sessions_json", arr.toString()).apply()
        // Always sync homescreen widget!
        ChallengeWidgetProvider.updateAllWidgets(context)
    }

    // Add session for any specific date
    fun addSession(sport: String, targetDateStr: String? = null): Boolean {
        val dateStr = targetDateStr ?: dateFormat.format(Date())
        val all = getAllSessions().toMutableList()

        val countOnDate = all.count { it.date == dateStr }
        if (countOnDate >= 2) {
            return false // Max 2 per day!
        }

        val session = WorkoutSession(
            id = "${System.currentTimeMillis()}-${(100..999).random()}",
            date = dateStr,
            time = timeFormat.format(Date()),
            sport = sport
        )
        all.add(session)
        saveSessions(all)
        return true
    }

    // Remove the most recent session of that sport on a specific date (or across week)
    fun removeSessionOnDate(sport: String, dateStr: String): Boolean {
        val all = getAllSessions().toMutableList()
        val index = all.indexOfLast { it.sport == sport && it.date == dateStr }
        if (index != -1) {
            all.removeAt(index)
            saveSessions(all)
            return true
        }
        return false
    }

    fun deleteSession(id: String) {
        val all = getAllSessions().filter { it.id != id }
        saveSessions(all)
    }

    fun resetWeek() {
        val bounds = getWeekBounds()
        val all = getAllSessions().filter { it.date < bounds.first || it.date > bounds.second }
        saveSessions(all)
    }

    fun getWeekBounds(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

        cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        val mon = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, 6)
        val sun = dateFormat.format(cal.time)
        return Pair(mon, sun)
    }

    fun getWeekDays(): List<DaySummary> {
        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(Date())
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)

        val plan = getPlanSettings()
        val sessions = getAllSessions()
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())

        val list = mutableListOf<DaySummary>()
        for (i in 0 until 7) {
            val dStr = dateFormat.format(cal.time)
            val dSessions = sessions.filter { it.date == dStr }
            val fit = dSessions.count { it.sport == "fitness" }
            val jog = dSessions.count { it.sport == "joggen" }

            list.add(
                DaySummary(
                    dateStr = dStr,
                    dayName = DAY_NAMES[i],
                    dayNumber = i,
                    dayOfMonth = dayFormat.format(cal.time),
                    isToday = dStr == todayStr,
                    fitnessCount = fit,
                    joggenCount = jog,
                    totalCount = fit + jog,
                    isPlanned = plan.mode == PlanMode.FIXED_DAYS && plan.fixedDays.contains(i)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    fun getWeekSessions(): List<WorkoutSession> {
        val bounds = getWeekBounds()
        return getAllSessions()
            .filter { it.date >= bounds.first && it.date <= bounds.second }
            .sortedByDescending { it.id }
    }

    // Plan Settings Management
    fun getPlanSettings(): PlanSettings {
        val modeStr = prefs.getString("plan_mode", PlanMode.SPORADIC.name) ?: PlanMode.SPORADIC.name
        val mode = try { PlanMode.valueOf(modeStr) } catch (e: Exception) { PlanMode.SPORADIC }
        val fit = prefs.getInt("target_fitness", 2)
        val jog = prefs.getInt("target_joggen", 2)
        val daysStr = prefs.getString("fixed_days", "1,3,5,6") ?: "1,3,5,6" // Di, Do, Sa, So default
        val fixedDays = daysStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

        return PlanSettings(
            mode = mode,
            targetFitness = fit,
            targetJoggen = jog,
            fixedDays = fixedDays
        )
    }

    fun savePlanSettings(settings: PlanSettings) {
        prefs.edit()
            .putString("plan_mode", settings.mode.name)
            .putInt("target_fitness", settings.targetFitness)
            .putInt("target_joggen", settings.targetJoggen)
            .putString("fixed_days", settings.fixedDays.joinToString(","))
            .apply()
        ChallengeWidgetProvider.updateAllWidgets(context)
        scheduleNextAlarm()
    }

    fun getWeekStatus(): WeekStatus {
        val bounds = getWeekBounds()
        val todayStr = dateFormat.format(Date())

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // 0 = Monday, ..., 6 = Sunday
        val todayIdx = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

        val sessions = getAllSessions().filter { it.date >= bounds.first && it.date <= bounds.second }

        val fitnessCount = sessions.count { it.sport == "fitness" }
        val joggenCount = sessions.count { it.sport == "joggen" }
        val totalCount = fitnessCount + joggenCount
        val remainingCount = maxOf(0, 4 - totalCount)
        val isCompleted = totalCount >= 4

        val todaySessions = sessions.filter { it.date == todayStr }
        val todayCount = todaySessions.size
        val todayCanAdd = todayCount < 2

        val remainingSlotsToday = maxOf(0, 2 - todayCount)
        val futureDays = maxOf(0, 6 - todayIdx)
        val maxPossibleRemaining = remainingSlotsToday + futureDays * 2

        val isImpossible = remainingCount > maxPossibleRemaining && !isCompleted
        val plan = getPlanSettings()
        val isTodayPlanned = plan.mode == PlanMode.FIXED_DAYS && plan.fixedDays.contains(todayIdx)

        val urgencyLevel: UrgencyLevel
        val urgencyHeadline: String
        val deadlineText: String
        val strategyAdvice: String
        val nagQuote: String

        if (isCompleted) {
            urgencyLevel = UrgencyLevel.COMPLETED
            urgencyHeadline = "🎉 WOCHENZIEL ERREICHT!"
            deadlineText = "4 von 4 Einheiten absolviert!"
            strategyAdvice = "Respekt! Du hast alle Pflicht-Einheiten für diese Woche sicher."
            nagQuote = "Saubere Arbeit Maschine! Ausruhen bis Montag."
        } else if (isImpossible) {
            urgencyLevel = UrgencyLevel.LOST
            urgencyHeadline = "💀 CHALLENGE VERLOREN!"
            deadlineText = "Noch $remainingCount nötig, aber nur $maxPossibleRemaining Plätze frei!"
            strategyAdvice = "Max 2 Einheiten pro Tag. Mathematisch nicht mehr schaffbar."
            nagQuote = "Verkackt! Spendier deinen Kumpels schon mal den Kasten!"
        } else if (maxPossibleRemaining == remainingCount) {
            urgencyLevel = UrgencyLevel.CRITICAL
            urgencyHeadline = "🚨 WORST-CASE NOTFALL!"
            if (todayIdx == 5) { // Samstag
                deadlineText = "Samstag 2x & Sonntag 2x Pflicht!"
                strategyAdvice = "Heute 2x (z.B. Joggen + Gym) & morgen 2x! Null Toleranz!"
                nagQuote = "ALARMSTUFE ROT! Heute 2x und morgen 2x, sonst hast du verkackt! Beweg dich!"
            } else if (todayIdx == 6) { // Sonntag
                deadlineText = "LETZTER TAG: Heute noch $remainingCount Einheiten nötig!"
                strategyAdvice = "Heute ist Sonntag! Du MUSST noch ran, sonst scheitert die Woche!"
                nagQuote = "Sonntagabend: Keine Ausreden mehr. Zieh die Laufschuhe JETZT an!"
            } else {
                deadlineText = "Ab jetzt JEDEN Tag das Maximum nötig!"
                strategyAdvice = "Jeder freie Slot muss genutzt werden."
                nagQuote = "Letzte Chance! Es brennt der Baum!"
            }
        } else if (plan.mode == PlanMode.FIXED_DAYS && isTodayPlanned && todayCount == 0) {
            // It's a planned day!
            urgencyLevel = UrgencyLevel.PLAN_DAY
            urgencyHeadline = "📅 TRAININGSPLAN: HEUTE EINHEIT!"
            deadlineText = "Heute ist dein festgelegter Trainingstag (${FULL_DAY_NAMES[todayIdx]})."
            strategyAdvice = "Plan-Ziel: ${plan.targetFitness}x Gym + ${plan.targetJoggen}x Joggen."
            nagQuote = "Laut deinem Plan musst du heute ran! Keine Faulheit – zieh durch!"
        } else if (maxPossibleRemaining <= remainingCount + 2) {
            urgencyLevel = UrgencyLevel.WARNING
            urgencyHeadline = "⚠️ PUFFER SCHMILZT!"
            val neededDays = Math.ceil(remainingCount / 2.0).toInt()
            val latestDayIdx = 7 - neededDays
            val latestName = FULL_DAY_NAMES.getOrElse(latestDayIdx) { "Samstag" }

            deadlineText = "Spätestens $latestName musst du starten!"
            strategyAdvice = "Mach heute mindestens 1x Sport, um am Wochenende nicht 2x ran zu müssen."
            nagQuote = "Dir fehlen noch $remainingCount Einheiten! Willst du am Wochenende sterben?!"
        } else {
            urgencyLevel = UrgencyLevel.CHILL
            urgencyHeadline = if (plan.mode == PlanMode.FIXED_DAYS) "📅 PLAN LÄUFT" else "😎 IM PLAN"
            deadlineText = if (plan.mode == PlanMode.FIXED_DAYS) {
                "Feste Tage geplant: ${plan.fixedDays.sorted().joinToString(", ") { DAY_NAMES[it] }}"
            } else {
                "Flexibel: Spätester Start Samstag (dann 2x Sa + 2x So)."
            }
            strategyAdvice = "Ziel: ${plan.targetFitness}x Fitness + ${plan.targetJoggen}x Joggen."
            nagQuote = "Alles locker, aber zieh heute durch, damit du am Wochenende frei hast!"
        }

        return WeekStatus(
            fitnessCount = fitnessCount,
            joggenCount = joggenCount,
            totalCount = totalCount,
            remainingCount = remainingCount,
            isCompleted = isCompleted,
            isImpossible = isImpossible,
            urgencyLevel = urgencyLevel,
            urgencyHeadline = urgencyHeadline,
            deadlineText = deadlineText,
            strategyAdvice = strategyAdvice,
            nagQuote = nagQuote,
            todayCount = todayCount,
            todayCanAdd = todayCanAdd,
            planSettings = plan
        )
    }

    fun scheduleNextAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NagAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (2 * 60 * 60 * 1000L) // 2 hours

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun showNagNotification() {
        val status = getWeekStatus()
        if (status.isCompleted) return

        createNotificationChannel()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: +1 Fitness for TODAY
        val fitnessIntent = Intent(context, QuickActionReceiver::class.java).apply {
            action = "ACTION_LOG_FITNESS"
        }
        val fitnessPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            fitnessIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: +1 Joggen for TODAY
        val joggenIntent = Intent(context, QuickActionReceiver::class.java).apply {
            action = "ACTION_LOG_JOGGEN"
        }
        val joggenPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            joggenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 600)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(status.urgencyHeadline)
            .setContentText("${status.deadlineText} „${status.nagQuote}“")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${status.deadlineText}\n\n„${status.nagQuote}“\n\nStand: ${status.totalCount}/4 (${status.fitnessCount}x Gym, ${status.joggenCount}x Joggen)")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(vibrationPattern)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)

        if (status.todayCanAdd) {
            builder.addAction(0, "🏋️ +1 Fitness", fitnessPendingIntent)
            builder.addAction(0, "🏃 +1 Joggen", joggenPendingIntent)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "4X Challenge Notfall-Alarm"
            val descriptionText = "Nervt dich offline und weckt dein Handy, solange Einheiten fehlen!"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 600)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
