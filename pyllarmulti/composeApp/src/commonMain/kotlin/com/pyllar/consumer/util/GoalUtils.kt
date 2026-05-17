package com.pyllar.consumer.util

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DayOfWeek

enum class GoalType {
    GOLD,
    SILVER,
    SAVINGS,
    SAVINGS_PLUS,
    FESTIVAL_SPENDS,
    VACATION,
    CHILDRENS_EDUCATION,
    GLOBAL_EXPOSURE,
    ALL_IN_ONE,
    OTHER
}

fun identifyGoalType(goalId: String): GoalType {
    if (goalId.isBlank()) return GoalType.OTHER

    val lowerGoalId = goalId.lowercase()
    return when {
        lowerGoalId == "gold" || lowerGoalId.contains("gold") -> GoalType.GOLD
        lowerGoalId == "silver" || lowerGoalId.contains("silver") -> GoalType.SILVER
        lowerGoalId == "savings" || lowerGoalId == "saving" || lowerGoalId.contains("saving") -> GoalType.SAVINGS
        lowerGoalId == "festival_spends" || lowerGoalId.contains("festival") -> GoalType.FESTIVAL_SPENDS
        lowerGoalId == "all_in_one" || lowerGoalId.contains("all_in_one") || lowerGoalId.contains("all-in-one") -> GoalType.ALL_IN_ONE
        lowerGoalId == "global_exposure" || lowerGoalId.contains("global_exposure") || lowerGoalId.contains("global-exposure") -> GoalType.GLOBAL_EXPOSURE
        lowerGoalId == "savings_plus" || lowerGoalId.contains("savings_plus") || lowerGoalId.contains("savings-plus") -> GoalType.SAVINGS_PLUS
        else -> GoalType.OTHER
    }
}

fun identifyGoalType(category: String?, schemeName: String?): GoalType {
    val cat = category?.uppercase() ?: ""
    val name = schemeName?.uppercase() ?: ""
    
    return when {
        cat == "GOLD" || name.contains("GOLD") -> GoalType.GOLD
        cat == "SILVER" || name.contains("SILVER") -> GoalType.SILVER
        cat == "SAVINGS_PLUS" -> GoalType.SAVINGS_PLUS
        cat == "SAVINGS" -> GoalType.SAVINGS
        cat == "FESTIVAL_SPENDS" -> GoalType.FESTIVAL_SPENDS
        cat == "VACATION" -> GoalType.VACATION
        cat == "CHILDRENS_EDUCATION" -> GoalType.CHILDRENS_EDUCATION
        cat == "GLOBAL_EXPOSURE" -> GoalType.GLOBAL_EXPOSURE
        cat == "ALL_IN_ONE" -> GoalType.ALL_IN_ONE
        else -> GoalType.OTHER
    }
}

fun getGoalDisplayName(goalType: GoalType): String {
    return when (goalType) {
        GoalType.GOLD -> "Gold"
        GoalType.SILVER -> "Silver"
        GoalType.SAVINGS -> "Savings"
        GoalType.FESTIVAL_SPENDS -> "Festivals"
        GoalType.ALL_IN_ONE -> "All-in-One"
        GoalType.GLOBAL_EXPOSURE -> "Global Exposure"
        GoalType.SAVINGS_PLUS -> "Savings Plus"
        else -> "Goal"
    }
}

fun formatRupeesShort(amount: Double): String {
    return when {
        amount >= 10_000_000 -> "₹${(amount / 10_000_000).toInt()}Cr"
        amount >= 100_000 -> "₹${(amount / 100_000).toInt()}L"
        else -> "₹${amount.toInt()}"
    }
}

fun calculateGoldReturns(dailyAmount: Double, years: Int, goalType: GoalType): Double {
    val days = years * 365
    val annualRate = when (goalType) {
        GoalType.GOLD -> 0.215
        GoalType.SILVER -> 0.295
        GoalType.SAVINGS -> 0.075
        GoalType.SAVINGS_PLUS -> 0.075
        GoalType.GLOBAL_EXPOSURE -> 0.23
        else -> 0.10
    }
    val dailyRate = (1.0 + annualRate).pow(1.0 / 365.0) - 1.0
    return if (dailyRate > 0) {
        dailyAmount * ((((1.0 + dailyRate).pow(days.toDouble()) - 1.0) / dailyRate) * (1.0 + dailyRate))
    } else {
        dailyAmount * days
    }
}

fun calculateLumpsumFutureValue(oneTimeAmount: Double, years: Int, goalType: GoalType = GoalType.OTHER): Double {
    val annualRate = when {
        goalType == GoalType.GOLD -> when (years) { 1 -> 0.754; 3 -> 0.342; 5 -> 0.221; 7 -> 0.215; else -> 0.215 }
        goalType == GoalType.SILVER -> when (years) { 1 -> 1.582; 3 -> 0.435; 5 -> 0.341; 7 -> 0.295; else -> 0.295 }
        goalType == GoalType.SAVINGS_PLUS -> 0.075
        goalType == GoalType.SAVINGS -> 0.075
        goalType == GoalType.FESTIVAL_SPENDS -> 0.075
        goalType == GoalType.GLOBAL_EXPOSURE -> 0.23
        goalType == GoalType.ALL_IN_ONE -> 0.175
        else -> 0.10
    }
    return (oneTimeAmount * (1.0 + annualRate).pow(years.toDouble())).coerceAtLeast(0.0)
}

fun getInvestmentStatus(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val dayOfWeek = now.dayOfWeek

    return when (dayOfWeek) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> "Tomorrow"
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY -> "Monday"
        DayOfWeek.SUNDAY -> "Tuesday"
        else -> "Tomorrow"
    }
}

fun formatDecimal(value: Double, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = (value * factor).toLong() / factor
    return rounded.toString()
}
