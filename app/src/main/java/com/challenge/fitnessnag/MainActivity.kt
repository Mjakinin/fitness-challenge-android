package com.challenge.fitnessnag

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.challenge.fitnessnag.databinding.ActivityMainBinding
import com.challenge.fitnessnag.databinding.ItemDayTabBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var challengeManager: ChallengeManager

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedDateStr: String = ""

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Reminder aktiviert (morgens, mittags, abends)! ✅", Toast.LENGTH_SHORT).show()
                challengeManager.scheduleNextAlarm()
            } else {
                Toast.makeText(this, "Ohne Berechtigung kann dich die App offline nicht erinnern! ⚠️", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        challengeManager = ChallengeManager(this)
        selectedDateStr = dateFormat.format(Date()) // default to today

        checkNotificationPermission()
        setupListeners()
        updateUI()

        // Schedule next smart alarm slot
        challengeManager.scheduleNextAlarm()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupListeners() {
        // Stepper for Selected Date: Fitness
        binding.btnPlusFitness.setOnClickListener {
            val ok = challengeManager.addSession("fitness", selectedDateStr)
            if (ok) {
                Toast.makeText(this, "🏋️ +1 Fitness gebucht!", Toast.LENGTH_SHORT).show()
                updateUI()
            } else {
                Toast.makeText(this, "⚠️ Limit erreicht! Max. 2 Einheiten an einem Tag erlaubt.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMinusFitness.setOnClickListener {
            val ok = challengeManager.removeSessionOnDate("fitness", selectedDateStr)
            if (ok) {
                Toast.makeText(this, "🏋️ 1x Fitness entfernt", Toast.LENGTH_SHORT).show()
                updateUI()
            } else {
                Toast.makeText(this, "Keine Fitness-Einheit an diesem Tag vorhanden.", Toast.LENGTH_SHORT).show()
            }
        }

        // Stepper for Selected Date: Joggen
        binding.btnPlusJoggen.setOnClickListener {
            val ok = challengeManager.addSession("joggen", selectedDateStr)
            if (ok) {
                Toast.makeText(this, "🏃 +1 Joggen gebucht!", Toast.LENGTH_SHORT).show()
                updateUI()
            } else {
                Toast.makeText(this, "⚠️ Limit erreicht! Max. 2 Einheiten an einem Tag erlaubt.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMinusJoggen.setOnClickListener {
            val ok = challengeManager.removeSessionOnDate("joggen", selectedDateStr)
            if (ok) {
                Toast.makeText(this, "🏃 1x Joggen entfernt", Toast.LENGTH_SHORT).show()
                updateUI()
            } else {
                Toast.makeText(this, "Keine Jogging-Einheit an diesem Tag vorhanden.", Toast.LENGTH_SHORT).show()
            }
        }

        // Open Plan Dialog
        binding.btnOpenPlanDialog.setOnClickListener { showPlanDialog() }
        binding.cardPlanAction.setOnClickListener { showPlanDialog() }

        // Reset Week
        binding.btnResetWeek.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Woche zurücksetzen?")
                .setMessage("Möchtest du wirklich alle eingetragenen Einheiten für die aktuelle Woche löschen?")
                .setPositiveButton("Zurücksetzen") { _, _ ->
                    challengeManager.resetWeek()
                    Toast.makeText(this, "Woche zurückgesetzt.", Toast.LENGTH_SHORT).show()
                    updateUI()
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }
    }

    private fun updateUI() {
        val status = challengeManager.getWeekStatus()
        val bounds = challengeManager.getWeekBounds()

        // Week Bounds
        val monParts = bounds.first.split("-")
        val sunParts = bounds.second.split("-")
        binding.tvWeekBounds.text = "Mo, ${monParts[2]}.${monParts[1]} – So, ${sunParts[2]}.${sunParts[1]}"

        // Weekly Total Score
        binding.tvTotalCount.text = "${status.totalCount}"

        // 4 Visual Segment Indicators
        val segments = listOf(binding.segUnit1, binding.segUnit2, binding.segUnit3, binding.segUnit4)
        val weekSessions = challengeManager.getWeekSessions()

        for (i in 0 until 4) {
            if (i < status.totalCount) {
                val s = weekSessions.getOrNull(weekSessions.size - 1 - i)
                if (s?.sport == "fitness") {
                    segments[i].setBackgroundColor(Color.parseColor("#A855F7")) // Purple
                } else {
                    segments[i].setBackgroundColor(Color.parseColor("#10B981")) // Emerald
                }
            } else {
                segments[i].setBackgroundColor(Color.parseColor("#1E293B")) // Dark unfilled
            }
        }

        // Urgency texts & card border
        binding.tvUrgencyHeadline.text = status.urgencyHeadline
        binding.tvDeadlineText.text = status.deadlineText

        when (status.urgencyLevel) {
            UrgencyLevel.CRITICAL -> {
                binding.cardUrgency.strokeColor = Color.parseColor("#EF4444")
                binding.tvUrgencyHeadline.setTextColor(Color.parseColor("#EF4444"))
            }
            UrgencyLevel.WARNING -> {
                binding.cardUrgency.strokeColor = Color.parseColor("#F97316")
                binding.tvUrgencyHeadline.setTextColor(Color.parseColor("#FB923C"))
            }
            UrgencyLevel.PLAN_DAY -> {
                binding.cardUrgency.strokeColor = Color.parseColor("#3B82F6")
                binding.tvUrgencyHeadline.setTextColor(Color.parseColor("#60A5FA"))
            }
            UrgencyLevel.COMPLETED -> {
                binding.cardUrgency.strokeColor = Color.parseColor("#10B981")
                binding.tvUrgencyHeadline.setTextColor(Color.parseColor("#34D399"))
            }
            else -> {
                binding.cardUrgency.strokeColor = Color.parseColor("#1E293B")
                binding.tvUrgencyHeadline.setTextColor(Color.parseColor("#FB923C"))
            }
        }

        if (status.isCompleted) {
            binding.tvRemainingStatus.text = "🎉 Ziel erreicht (4/4)!"
            binding.tvRemainingStatus.setTextColor(ContextCompat.getColor(this, R.color.emerald_primary))
        } else if (status.isImpossible) {
            binding.tvRemainingStatus.text = "💀 Challenge verloren!"
            binding.tvRemainingStatus.setTextColor(ContextCompat.getColor(this, R.color.red_alert))
        } else {
            binding.tvRemainingStatus.text = "Noch ${status.remainingCount} ${if (status.remainingCount == 1) "Einheit" else "Einheiten"} nötig"
            binding.tvRemainingStatus.setTextColor(ContextCompat.getColor(this, R.color.orange_light))
        }

        // Coach Advice Box
        val advice = status.coachAdvice
        binding.tvAdviceHeadline.text = advice.headline
        binding.tvAdviceDetail.text = advice.detail
        binding.tvAdviceBadge.text = advice.badge
        try {
            binding.tvAdviceBadge.setTextColor(Color.parseColor(advice.badgeColorHex))
            binding.cardCoachAdvice.strokeColor = Color.parseColor(advice.badgeColorHex)
        } catch (_: Exception) {}

        // Render Day Tabs (Mo - So)
        renderDayTabs()

        // Render Stepper for Selected Day
        updateSelectedDaySection()

        // Render Plan & Split Details
        val preset = status.activePreset
        binding.tvPlanOverview.text = "${preset.badge} ${preset.title}"
        binding.tvPlanSplitDetails.text = preset.splitDescription

        // Render Session History
        renderSessionHistory(weekSessions)
    }

    private fun renderDayTabs() {
        binding.layoutDayTabs.removeAllViews()
        val days = challengeManager.getWeekDays()

        for (day in days) {
            val tabBinding = ItemDayTabBinding.inflate(layoutInflater, binding.layoutDayTabs, false)

            tabBinding.tvTabDayName.text = day.dayName
            tabBinding.tvTabDayNumber.text = day.dayOfMonth

            val isSelected = day.dateStr == selectedDateStr
            tabBinding.dayTabRoot.setBackgroundResource(
                if (isSelected) R.drawable.bg_day_tab_selected else R.drawable.bg_day_tab_unselected
            )

            // Dot / Count badge
            if (day.totalCount > 0) {
                tabBinding.tvTabDot.visibility = View.VISIBLE
                tabBinding.tvTabDot.text = "${day.totalCount}"
                tabBinding.tvTabDot.setTextColor(
                    if (day.totalCount >= 2) Color.parseColor("#F97316") else Color.parseColor("#A855F7")
                )
            } else if (day.isPlanned) {
                tabBinding.tvTabDot.visibility = View.VISIBLE
                tabBinding.tvTabDot.text = "★"
                tabBinding.tvTabDot.setTextColor(Color.parseColor("#3B82F6"))
            } else {
                tabBinding.tvTabDot.visibility = View.INVISIBLE
            }

            if (day.isToday) {
                tabBinding.tvTabDayName.setTextColor(Color.parseColor("#EA580C"))
            } else {
                tabBinding.tvTabDayName.setTextColor(Color.parseColor("#94A3B8"))
            }

            tabBinding.dayTabRoot.setOnClickListener {
                selectedDateStr = day.dateStr
                updateUI()
            }

            binding.layoutDayTabs.addView(tabBinding.root)
        }
    }

    private fun updateSelectedDaySection() {
        val days = challengeManager.getWeekDays()
        val currentDay = days.find { it.dateStr == selectedDateStr } ?: days.first()

        val fullDayName = ChallengeManager.FULL_DAY_NAMES.getOrElse(currentDay.dayNumber) { "Tag" }
        val dateParts = currentDay.dateStr.split("-")
        val niceDate = if (dateParts.size == 3) "${dateParts[2]}.${dateParts[1]}." else currentDay.dateStr

        val todayTag = if (currentDay.isToday) " (HEUTE)" else ""
        binding.tvSelectedDayTitle.text = "🗓️ $fullDayName, $niceDate$todayTag"
        binding.tvSelectedDayBadge.text = "${currentDay.totalCount} / 2 Einheiten"

        // Counts on selected date
        binding.tvFitnessCount.text = "${currentDay.fitnessCount}"
        binding.tvJoggenCount.text = "${currentDay.joggenCount}"

        // Minus button states
        binding.btnMinusFitness.isEnabled = currentDay.fitnessCount > 0
        binding.btnMinusFitness.alpha = if (currentDay.fitnessCount > 0) 1.0f else 0.35f

        binding.btnMinusJoggen.isEnabled = currentDay.joggenCount > 0
        binding.btnMinusJoggen.alpha = if (currentDay.joggenCount > 0) 1.0f else 0.35f

        // Plus button states (enforces max 2 per day)
        val canAdd = currentDay.totalCount < 2
        binding.btnPlusFitness.isEnabled = canAdd
        binding.btnPlusJoggen.isEnabled = canAdd
        binding.btnPlusFitness.alpha = if (canAdd) 1.0f else 0.35f
        binding.btnPlusJoggen.alpha = if (canAdd) 1.0f else 0.35f

        if (!canAdd) {
            binding.tvSelectedDayBadge.setTextColor(Color.parseColor("#EF4444"))
        } else {
            binding.tvSelectedDayBadge.setTextColor(Color.parseColor("#94A3B8"))
        }
    }

    private fun renderSessionHistory(sessions: List<WorkoutSession>) {
        binding.layoutSessionList.removeAllViews()

        if (sessions.isEmpty()) {
            binding.tvEmptyHistory.visibility = View.VISIBLE
            return
        }

        binding.tvEmptyHistory.visibility = View.GONE

        for (session in sessions) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 10)
            }

            val tvIcon = TextView(this).apply {
                text = if (session.sport == "fitness") "🏋️" else "🏃"
                textSize = 16f
                setPadding(0, 0, 12, 0)
            }

            val tvDetails = TextView(this).apply {
                val parts = session.date.split("-")
                val dateNice = if (parts.size == 3) "${parts[2]}.${parts[1]}." else session.date
                val sportName = if (session.sport == "fitness") "Fitness (Gym)" else "Joggen (Lauf)"
                text = "$sportName • $dateNice ${session.time} Uhr"
                textSize = 12f
                setTextColor(Color.parseColor("#E2E8F0"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            }

            val btnDelete = ImageView(this).apply {
                setImageResource(R.drawable.ic_delete)
                contentDescription = "Löschen"
                setPadding(12, 6, 6, 6)
                setOnClickListener {
                    challengeManager.deleteSession(session.id)
                    Toast.makeText(context, "Einheit gelöscht.", Toast.LENGTH_SHORT).show()
                    updateUI()
                }
            }

            row.addView(tvIcon)
            row.addView(tvDetails)
            row.addView(btnDelete)

            binding.layoutSessionList.addView(row)
        }
    }

    private fun showPlanDialog() {
        val currentPlan = challengeManager.getPlanSettings()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plan_settings, null)

        val rgMode = dialogView.findViewById<RadioGroup>(R.id.rgPlanMode)
        val layoutFixedDays = dialogView.findViewById<LinearLayout>(R.id.layoutFixedDays)
        val rgPresets = dialogView.findViewById<RadioGroup>(R.id.rgPresets)

        val dayButtons = listOf(
            dialogView.findViewById<ToggleButton>(R.id.tbMon),
            dialogView.findViewById<ToggleButton>(R.id.tbTue),
            dialogView.findViewById<ToggleButton>(R.id.tbWed),
            dialogView.findViewById<ToggleButton>(R.id.tbThu),
            dialogView.findViewById<ToggleButton>(R.id.tbFri),
            dialogView.findViewById<ToggleButton>(R.id.tbSat),
            dialogView.findViewById<ToggleButton>(R.id.tbSun)
        )

        // Set mode
        if (currentPlan.mode == PlanMode.FIXED_DAYS) {
            rgMode.check(R.id.rbFixedDays)
            layoutFixedDays.visibility = View.VISIBLE
        } else {
            rgMode.check(R.id.rbSporadic)
            layoutFixedDays.visibility = View.GONE
        }

        rgMode.setOnCheckedChangeListener { _, checkedId ->
            layoutFixedDays.visibility = if (checkedId == R.id.rbFixedDays) View.VISIBLE else View.GONE
        }

        // Set day toggles
        for (i in dayButtons.indices) {
            dayButtons[i].isChecked = currentPlan.fixedDays.contains(i)
        }

        // Populate Presets list
        var selectedPresetId = currentPlan.presetId

        for (preset in ChallengeManager.PRESETS) {
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                tag = preset.id
                text = "${preset.title}\n${preset.splitDescription}"
                textSize = 12f
                setTextColor(Color.parseColor("#F8FAFC"))
                buttonTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.orange_primary)
                setPadding(16, 12, 16, 12)
                layoutParams = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 8)
                }
            }

            if (preset.id == selectedPresetId) {
                rb.isChecked = true
            }

            rb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedPresetId = preset.id
                }
            }

            rgPresets.addView(rb)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.btnCancelPlan).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnSavePlan).setOnClickListener {
            val selectedMode = if (rgMode.checkedRadioButtonId == R.id.rbFixedDays) {
                PlanMode.FIXED_DAYS
            } else {
                PlanMode.SPORADIC
            }

            val selectedDays = mutableSetOf<Int>()
            for (i in dayButtons.indices) {
                if (dayButtons[i].isChecked) selectedDays.add(i)
            }

            val chosenPreset = ChallengeManager.PRESETS.find { it.id == selectedPresetId }
                ?: ChallengeManager.PRESETS.first()

            challengeManager.savePlanSettings(
                PlanSettings(
                    mode = selectedMode,
                    presetId = chosenPreset.id,
                    targetFitness = chosenPreset.fitness,
                    targetJoggen = chosenPreset.joggen,
                    fixedDays = selectedDays
                )
            )

            Toast.makeText(this, "Split gespeichert: ${chosenPreset.title} ✅", Toast.LENGTH_SHORT).show()
            updateUI()
            dialog.dismiss()
        }

        dialog.show()
    }
}
