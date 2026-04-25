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
