package com.challenge.fitnessnag

data class WorkoutSession(
    val id: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val sport: String // "fitness" or "joggen"
)

enum class UrgencyLevel {
    CHILL,
    PLAN_DAY,
    NOTICE,
    WARNING,
    CRITICAL,
    LOST,
    COMPLETED
}

enum class PlanMode {
    SPORADIC,
    FIXED_DAYS
}

data class PlanSettings(
    val mode: PlanMode,
    val targetFitness: Int,
    val targetJoggen: Int,
    val fixedDays: Set<Int> // 0 = Mon, 1 = Tue, ..., 6 = Sun
)

data class DaySummary(
    val dateStr: String, // YYYY-MM-DD
    val dayName: String, // Mo, Di, Mi...
    val dayNumber: Int,  // 0 = Mo, 6 = So
    val dayOfMonth: String, // e.g. "27"
    val isToday: Boolean,
    val fitnessCount: Int,
    val joggenCount: Int,
    val totalCount: Int,
    val isPlanned: Boolean
)

data class WeekStatus(
    val fitnessCount: Int,
    val joggenCount: Int,
    val totalCount: Int,
    val remainingCount: Int,
    val isCompleted: Boolean,
    val isImpossible: Boolean,
    val urgencyLevel: UrgencyLevel,
    val urgencyHeadline: String,
    val deadlineText: String,
    val strategyAdvice: String,
    val nagQuote: String,
    val todayCount: Int,
    val todayCanAdd: Boolean,
    val planSettings: PlanSettings
)
