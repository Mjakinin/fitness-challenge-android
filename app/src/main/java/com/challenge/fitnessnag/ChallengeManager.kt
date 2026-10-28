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

        val PRESETS = listOf(
            TrainingPreset(
                id = "athlet",
                title = "Der Athlet (2x Gym + 2x Run)",
                subtitle = "Optimaler Muskelaufbau & Ausdauer",
                fitness = 2,
                joggen = 2,
                badge = "🔥 50 / 50",
                splitDescription = "Mo: Oberkörper • Mi: Tempo-Lauf • Fr: Unterkörper • So: Ausdauerlauf",
                idealDays = "Mo, Mi, Fr, So"
            ),
            TrainingPreset(
                id = "push_pull",
                title = "Push / Pull + 2x Run",
                subtitle = "Oberkörper-Split & 2 Laufeinheiten",
                fitness = 2,
                joggen = 2,
                badge = "💪 Push / Pull",
                splitDescription = "Di: Push (Brust/Schulter/Trizeps) • Do: Pull (Rücken/Bizeps) • Sa: Intervall • So: Run",
                idealDays = "Di, Do, Sa, So"
            ),
            TrainingPreset(
                id = "weekend_free",
                title = "Wochenend-Schoner (Mo-Fr)",
                subtitle = "Unter der Woche ballern, Sa & So frei!",
                fitness = 2,
                joggen = 2,
                badge = "🏖️ Sa/So Frei",
                splitDescription = "Mo: Gym • Di: Run • Do: Gym • Fr: Run ➔ Wochenende komplett frei!",
                idealDays = "Mo, Di, Do, Fr"
            ),
            TrainingPreset(
                id = "gym_focus",
                title = "Gym-Fokus (3x Kraft + 1x Run)",
                subtitle = "3er Split Hypertrophie + 1x Alibi-Cardio",
                fitness = 3,
                joggen = 1,
                badge = "🏋️ 3x Gym",
                splitDescription = "Mo: Push • Mi: Pull • Fr: Beine • Sa: 5km Cardio",
                idealDays = "Mo, Mi, Fr, Sa"
            ),
            TrainingPreset(
                id = "run_focus",
                title = "Lauf-Fokus (3x Run + 1x Gym)",
                subtitle = "Ausdauer-Fokus + 1x Kraftschutz",
                fitness = 1,
                joggen = 3,
                badge = "🏃 3x Run",
                splitDescription = "Di: Easy Run • Do: Intervalle • Sa: Ganzkörper-Kraft • So: Longrun",
                idealDays = "Di, Do, Sa, So"
            ),
            TrainingPreset(
                id = "all_gym",
                title = "Pures Eisen (4x Kraft, 0x Run)",
                subtitle = "Reines Krafttraining / 4er Split",
                fitness = 4,
                joggen = 0,
                badge = "🦾 4x Eisen",
                splitDescription = "Mo: Oberkörper A • Di: Unterkörper A • Do: Oberkörper B • Fr: Unterkörper B",
                idealDays = "Mo, Di, Do, Fr"
            ),
            TrainingPreset(
                id = "all_run",
                title = "Reine Laufwoche (0x Gym, 4x Run)",
                subtitle = "Nur Asphalt & Waldwege",
                fitness = 0,
                joggen = 4,
                badge = "⚡ 4x Laufen",
                splitDescription = "Mo: Easy • Mi: Tempo • Fr: Intervalle • So: Distanzlauf",
                idealDays = "Mo, Mi, Fr, So"
            )
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
        ChallengeWidgetProvider.updateAllWidgets(context)
        scheduleNextAlarm()
    }

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
        val presetId = prefs.getString("preset_id", "athlet") ?: "athlet"
        val fit = prefs.getInt("target_fitness", 2)
        val jog = prefs.getInt("target_joggen", 2)
        val daysStr = prefs.getString("fixed_days", "1,3,5,6") ?: "1,3,5,6"
        val fixedDays = daysStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

        return PlanSettings(
            mode = mode,
            presetId = presetId,
            targetFitness = fit,
            targetJoggen = jog,
            fixedDays = fixedDays
        )
    }

    fun savePlanSettings(settings: PlanSettings) {
        prefs.edit()
            .putString("plan_mode", settings.mode.name)
            .putString("preset_id", settings.presetId)
            .putInt("target_fitness", settings.targetFitness)
            .putInt("target_joggen", settings.targetJoggen)
            .putString("fixed_days", settings.fixedDays.joinToString(","))
            .apply()
        ChallengeWidgetProvider.updateAllWidgets(context)
        scheduleNextAlarm()
    }

    fun getActivePreset(): TrainingPreset {
        val plan = getPlanSettings()
        return PRESETS.find { it.id == plan.presetId }
            ?: TrainingPreset(
                id = "custom",
                title = "Individueller Split",
                subtitle = "${plan.targetFitness}x Gym + ${plan.targetJoggen}x Joggen",
                fitness = plan.targetFitness,
                joggen = plan.targetJoggen,
                badge = "⚙️ Custom",
                splitDescription = "Flexible Verteilung über die Woche",
                idealDays = "Flexibel"
            )
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
        val activePreset = getActivePreset()
        val isTodayPlanned = plan.mode == PlanMode.FIXED_DAYS && plan.fixedDays.contains(todayIdx)

        val urgencyLevel: UrgencyLevel
        val urgencyHeadline: String
        val deadlineText: String
        val strategyAdvice: String
        val nagQuote: String

        if (isCompleted) {
            urgencyLevel = UrgencyLevel.COMPLETED
            urgencyHeadline = "🎉 4/4 ERLEDIGT!"
            deadlineText = "Wochenziel erreicht!"
            strategyAdvice = "Starke Woche! Regeneration bis Montag."
            nagQuote = "Saubere Arbeit Maschine! Beine hochlegen."
        } else if (isImpossible) {
            urgencyLevel = UrgencyLevel.LOST
            urgencyHeadline = "💀 CHALLENGE VERLOREN!"
            deadlineText = "Noch $remainingCount nötig, aber max. $maxPossibleRemaining Plätze frei!"
            strategyAdvice = "Max 2 Einheiten pro Tag. Rechnerisch nicht mehr schaffbar."
            nagQuote = "Verkackt! Spendier deinen Kumpels schon mal den Kasten!"
        } else if (maxPossibleRemaining == remainingCount) {
            urgencyLevel = UrgencyLevel.CRITICAL
            urgencyHeadline = "🚨 WORST-CASE NOTFALL!"
            if (todayIdx == 5) { // Samstag
                deadlineText = "Heute 2x & morgen 2x Pflicht!"
                strategyAdvice = "Heute Run + Gym und morgen nochmal! 0 Spielraum!"
                nagQuote = "ALARMSTUFE ROT! Heute 2x & morgen 2x, sonst hast du verloren!"
            } else if (todayIdx == 6) { // Sonntag
                deadlineText = "LETZTER TAG: Heute noch $remainingCount Einheiten!"
                strategyAdvice = "Heute ist Sonntag! Zieh jetzt durch!"
                nagQuote = "Sonntagabend: Keine Ausreden mehr. Zieh die Schuhe an!"
            } else {
                deadlineText = "Ab jetzt jeden Tag Maximum!"
                strategyAdvice = "Jeder freie Slot muss genutzt werden."
                nagQuote = "Es brennt der Baum! Keine Pausen mehr möglich."
            }
        } else if (plan.mode == PlanMode.FIXED_DAYS && isTodayPlanned && todayCount == 0) {
            urgencyLevel = UrgencyLevel.PLAN_DAY
            urgencyHeadline = "📅 TRAININGS-TAG"
            deadlineText = "Heute laut Plan: Einheit fällig (${FULL_DAY_NAMES[todayIdx]})."
            strategyAdvice = "Plan: ${activePreset.title}"
            nagQuote = "Laut deinem Plan musst du heute ran! Keine Faulheit."
        } else if (maxPossibleRemaining <= remainingCount + 2) {
            urgencyLevel = UrgencyLevel.WARNING
            urgencyHeadline = "⚠️ PUFFER SCHMILZT!"
            val neededDays = Math.ceil(remainingCount / 2.0).toInt()
            val latestDayIdx = 7 - neededDays
            val latestName = FULL_DAY_NAMES.getOrElse(latestDayIdx) { "Samstag" }

            deadlineText = "Spätestens $latestName starten!"
            strategyAdvice = "Heute mindestens 1x Sport, um Sa/So Doppel-Schicht zu vermeiden."
            nagQuote = "Dir fehlen noch $remainingCount Einheiten! Lass es nicht schleifen."
        } else {
            urgencyLevel = UrgencyLevel.CHILL
            urgencyHeadline = if (plan.mode == PlanMode.FIXED_DAYS) "📅 PLAN LÄUFT" else "😎 IM PLAN"
            deadlineText = if (plan.mode == PlanMode.FIXED_DAYS) {
                "Feste Tage: ${plan.fixedDays.sorted().joinToString(", ") { DAY_NAMES[it] }}"
            } else {
                "Spätester Notfall-Start: Samstag (dann 2x Sa + 2x So)."
            }
            strategyAdvice = "Split: ${activePreset.title}"
            nagQuote = "Alles locker. Zieh heute durch, dann hast du am Wochenende frei!"
        }

        val coachAdvice = generateCoachAdvice(
            todayIdx, remainingCount, fitnessCount, joggenCount,
            plan.targetFitness, plan.targetJoggen, todayCount, isCompleted, isImpossible
        )

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
            planSettings = plan,
            coachAdvice = coachAdvice,
            activePreset = activePreset
        )
    }

    private fun generateCoachAdvice(
        todayIdx: Int,
        remaining: Int,
        fitDone: Int,
        jogDone: Int,
        fitTarget: Int,
        jogTarget: Int,
        todayCount: Int,
        isCompleted: Boolean,
        isImpossible: Boolean
    ): CoachAdvice {
        if (isCompleted) {
            return CoachAdvice(
                headline = "🎉 Wochenziel 4/4 erreicht!",
                detail = "Starke Leistung! Du kannst dich bis Montag komplett ausruhen.",
                badge = "ERLEDIGT",
                badgeColorHex = "#10B981"
            )
        }
        if (isImpossible) {
            return CoachAdvice(
                headline = "💀 Challenge rechnerisch verloren",
                detail = "Maximal 2 Einheiten pro Tag. Nächste Woche früher anfangen!",
                badge = "VERLOREN",
                badgeColorHex = "#EF4444"
            )
        }
        if (todayCount >= 2) {
            return CoachAdvice(
                headline = "✅ Tageslimit erreicht (2/2)",
                detail = "Perfekt gearbeitet! Gönn dir Regeneration. Morgen geht es weiter.",
                badge = "REGENERATION",
                badgeColorHex = "#3B82F6"
            )
        }
        if (todayIdx == 5 && remaining == 4) { // Samstag 0 Einheiten
            return CoachAdvice(
                headline = "🚨 NOTFALL-PLAN: Heute 2x & morgen 2x!",
                detail = "Vormittags 🏃 1x Run & abends 🏋️ 1x Gym, sonst verlierst du die Challenge!",
                badge = "NOTFALL",
                badgeColorHex = "#EF4444"
            )
        }
        if (todayIdx == 4 && remaining >= 3) { // Freitag
            return CoachAdvice(
                headline = "⚠️ Heute 1x Sport empfohlen!",
                detail = "Wenn du heute skippst, MUSST du Sa & So jeweils doppelt ran (Worst Case).",
                badge = "DRINGEND",
                badgeColorHex = "#F97316"
            )
        }
        if (fitDone < fitTarget && jogDone >= jogTarget) {
            return CoachAdvice(
                headline = "🏋️ Heute empfohlen: 1x Fitness (Gym)",
                detail = "Dein Lauf-Konto ist erfüllt ($jogDone/$jogTarget). Heute Krafttraining!",
                badge = "OPTIMAL",
                badgeColorHex = "#A855F7"
            )
        }
        if (jogDone < jogTarget && fitDone >= fitTarget) {
            return CoachAdvice(
                headline = "🏃 Heute empfohlen: 1x Joggen",
                detail = "Dein Gym-Konto ist erfüllt ($fitDone/$fitTarget). Heute Laufschuhe an!",
                badge = "OPTIMAL",
                badgeColorHex = "#10B981"
            )
        }
        // Balanced
        val nextSport = if (fitDone <= jogDone) "🏋️ 1x Fitness (Kraft)" else "🏃 1x Joggen (Lauf)"
        return CoachAdvice(
            headline = "💡 Heute empfohlen: $nextSport",
            detail = "Hält deine Balance und sichert dir ein entspanntes Wochenende.",
            badge = "EMPFEHLUNG",
            badgeColorHex = "#FB923C"
        )
    }

    // Smart Alarm Scheduler: Fixed meaningful times (Morning 07:00, Lunch 12:30, Evening 18:30)
    fun scheduleNextAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NagAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val status = getWeekStatus()
        val nextTriggerMillis = calculateNextAlarmTime(status)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMillis,
                pendingIntent
            )
        }
    }

    private fun calculateNextAlarmTime(status: WeekStatus): Long {
        val now = Calendar.getInstance()

        // 1. If 4/4 completed: completely silent until next Monday morning at 08:00
        if (status.isCompleted) {
            val nextMon = Calendar.getInstance()
            val dayOfWeek = nextMon.get(Calendar.DAY_OF_WEEK)
            val daysUntilMon = if (dayOfWeek == Calendar.SUNDAY) 1 else (9 - dayOfWeek)
            nextMon.add(Calendar.DAY_OF_YEAR, daysUntilMon)
            nextMon.set(Calendar.HOUR_OF_DAY, 8)
            nextMon.set(Calendar.MINUTE, 0)
            nextMon.set(Calendar.SECOND, 0)
            nextMon.set(Calendar.MILLISECOND, 0)
            return nextMon.timeInMillis
        }

        // 2. Define notification slots (hours, minutes) based on urgency
        val slots: List<Pair<Int, Int>> = when (status.urgencyLevel) {
            UrgencyLevel.CRITICAL -> {
                // Saturday 0 units or emergency: 07:00, 11:30, 15:30, 18:30, 21:00
                listOf(
                    Pair(7, 0),
                    Pair(11, 30),
                    Pair(15, 30),
                    Pair(18, 30),
                    Pair(21, 0)
                )
            }
            UrgencyLevel.WARNING -> {
                // Buffer melting: Morning, Lunch, Evening
                listOf(
                    Pair(7, 0),
                    Pair(12, 30),
                    Pair(18, 30)
                )
            }
            else -> {
                // Chill / Normal: Morning 07:30 & Evening 18:30
                listOf(
                    Pair(7, 30),
                    Pair(18, 30)
                )
            }
        }

        // If user already logged 2 units today, skip all remaining slots today
        val skipToday = status.todayCount >= 2

        if (!skipToday) {
            for (slot in slots) {
                val candidate = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, slot.first)
                    set(Calendar.MINUTE, slot.second)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // Minimum 2 minutes in future
                if (candidate.timeInMillis > now.timeInMillis + 2 * 60 * 1000L) {
                    return candidate.timeInMillis
                }
            }
        }

        // If all slots today have passed (or today full), take the first slot of tomorrow
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            val firstSlot = slots.first()
            set(Calendar.HOUR_OF_DAY, firstSlot.first)
            set(Calendar.MINUTE, firstSlot.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return tomorrow.timeInMillis
    }

    // Short, crisp notification: not overloaded with text, clear status + short recommendation
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

        // Action: +1 Fitness right from notification bar
        val fitnessIntent = Intent(context, QuickActionReceiver::class.java).apply {
            action = "ACTION_LOG_FITNESS"
        }
        val fitnessPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            fitnessIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: +1 Joggen right from notification bar
        val joggenIntent = Intent(context, QuickActionReceiver::class.java).apply {
            action = "ACTION_LOG_JOGGEN"
        }
        val joggenPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            joggenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crisp, short notification copy
        val title = when (status.urgencyLevel) {
            UrgencyLevel.CRITICAL -> "🚨 NOTFALL • ${status.totalCount}/4 Einheiten"
            UrgencyLevel.WARNING -> "⚠️ 4X Stand: ${status.totalCount}/4 geschafft"
            UrgencyLevel.PLAN_DAY -> "📅 Trainingstag • ${status.totalCount}/4"
            else -> "⚡ 4X Stand: ${status.totalCount}/4 Einheiten"
        }

        val shortMessage = if (status.urgencyLevel == UrgencyLevel.CRITICAL && status.totalCount == 0) {
            "Samstag & 0 Einheiten! Heute Run + Gym nötig, sonst Challenge verloren!"
        } else {
            "${status.coachAdvice.headline} • ${status.coachAdvice.detail}"
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 600)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(shortMessage)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
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
            val name = "4X Challenge Reminder"
            val descriptionText = "Erinnert dich gezielt morgens, mittags & abends."
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
