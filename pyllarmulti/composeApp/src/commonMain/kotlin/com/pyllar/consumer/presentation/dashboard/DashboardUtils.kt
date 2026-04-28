package com.pyllar.consumer.presentation.dashboard

import androidx.compose.ui.graphics.Color

fun formatGoalName(name: String): String {
    return name.replace("_", " ").split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

fun getCorrelationColorForCategory(category: String?, colorTheme: String?): Color {
    return when (category?.uppercase()) {
        "GOLD" -> Color(0xFFFFD700)
        "SILVER" -> Color(0xFFC0C0C0)
        "SAVINGS" -> Color(0xFF008080)
        "FESTIVAL_SPENDS" -> Color(0xFFFF8C00)
        "RETIREMENT" -> Color(0xFF2E7D32)
        "CHILDRENS_EDUCATION" -> Color(0xFF1565C0)
        "VACATION" -> Color(0xFF7B1FA2)
        else -> {
            when (colorTheme?.lowercase()) {
                "blue" -> Color(0xFF1565C0)
                "green" -> Color(0xFF2E7D32)
                "orange" -> Color(0xFFFF8C00)
                "purple" -> Color(0xFF7B1FA2)
                "teal" -> Color(0xFF008080)
                else -> Color(0xFF2E7D32)
            }
        }
    }
}

fun formatSchemeName(name: String): String {
    return name.replace("Direct Plan", "").replace("Growth", "").trim()
}

fun formatIndian(value: Double): String {
    val longVal = kotlin.math.round(value).toLong()
    val negative = longVal < 0
    val s = kotlin.math.abs(longVal).toString()
    if (s.length <= 3) return if (negative) "-$s" else s
    val last3 = s.takeLast(3)
    val rest = s.dropLast(3)
    val grouped = buildString {
        for ((i, c) in rest.reversed().withIndex()) {
            if (i > 0 && i % 2 == 0) append(',')
            append(c)
        }
    }.reversed()
    val result = "$grouped,$last3"
    return if (negative) "-$result" else result
}

fun formatPercent(value: Double, decimals: Int = 2): String {
    val factor = when (decimals) {
        1 -> 10.0
        2 -> 100.0
        else -> 100.0
    }
    val rounded = (value * factor).toLong() / factor
    return rounded.toString()
}

fun formatKycStatus(status: String): String {
    return when (status.uppercase()) {
        "PENDING" -> "KYC Pending"
        "IN_PROGRESS" -> "KYC In Progress"
        "EXPIRED" -> "KYC Expired"
        "REJECTED" -> "KYC Rejected"
        "SUCCESS", "COMPLETED" -> "KYC Verified"
        else -> status
    }
}

fun ceil(value: Double): Double {
    return kotlin.math.ceil(value)
}

fun formatWeight(value: Double): String {
    val rounded = (kotlin.math.round(value * 100) / 100.0)
    return "$rounded g"
}
