package com.pyllar.consumer.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import pyllar.composeapp.generated.resources.*
import com.pyllar.consumer.presentation.ui.theme.V2SuccessGreen
import org.jetbrains.compose.resources.DrawableResource
import com.pyllar.consumer.util.*
import kotlin.math.pow


fun formatGoalName(name: String): String {
    return name.replace("_", " ").split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

fun getCorrelationColorForCategory(category: String?, colorTheme: String?): Color {
    // Use colorTheme if available, otherwise use category-based colors
    val themeColor = colorTheme?.toColor()
    if (themeColor != null && themeColor != V2SuccessGreen) { // If it's not the default green
        return themeColor
    }

    return when (category?.uppercase()) {
        "GOLD" -> Color(0xFFA27915) // Gold color
        "SILVER" -> Color(0xFF818181) // Silver/gray color
        "FESTIVAL_SPENDS" -> Color(0xFFFF9800) // Orange
        "CHILDRENS_EDUCATION" -> Color(0xFF2196F3) // Blue
        "VACATION" -> Color(0xFF9C27B0) // Purple
        "SAVINGS" -> Color(0xFF388E3C) // Green
        "SAVINGS_PLUS" -> Color(0xFF2E7D32) // Emerald Green
        "GLOBAL_EXPOSURE" -> Color(0xFF00897B) // Teal
        "ALL_IN_ONE" -> Color(0xFF2C4C9C) // Dark blue
        else -> V2SuccessGreen // Default green
    }
}

fun getBorderColorForCategory(category: String?): Color {
    return when (category?.uppercase()) {
        "GOLD" -> Color(0xFFFFD700)
        "SILVER" -> Color(0xFFC0C0C0)
        "FESTIVAL_SPENDS" -> Color(0xFFFF9800)
        "CHILDRENS_EDUCATION" -> Color(0xFF2196F3)
        "VACATION" -> Color(0xFF9C27B0)
        "SAVINGS" -> V2SuccessGreen
        "GLOBAL_EXPOSURE" -> Color(0xFF00897B)
        "ALL_IN_ONE" -> Color(0xFF2C4C9C) // Match correlation dark blue
        else -> V2SuccessGreen
    }
}

fun getDarkBorderColorForCategory(category: String?, colorTheme: String?): Color {
    val cat = category?.uppercase().orEmpty()
    if (cat == "SAVINGS_PLUS") return Color(0xFF1B5E20) // Forest Green
    
    val baseColor = colorTheme?.toColor() ?: getBorderColorForCategory(category)
    return baseColor.copy(alpha = 0.8f)
}

fun getGoalGradientColors(category: String?, colorTheme: String?): List<Color> {
    return when (category?.uppercase()) {
        "GOLD" -> listOf(
            Color(0xFFFFF9E6), // Rich golden cream
            Color(0xFFFFF4D6), // Warm golden yellow
            Color(0xFFFFE8B8)  // Vibrant golden amber
        )
        "SILVER" -> listOf(
            Color(0xFFFAFAFA), // White smoke
            Color(0xFFF5F5F5), // Very light grey
            Color(0xFFE8E8E8)  // Light silver
        )
        "FESTIVAL_SPENDS" -> listOf(
            Color(0xFFFFF5F5), // Very light pink-red
            Color(0xFFFFE8E8), // Light rose
            Color(0xFFFFD7B5)  // Soft light red
        )
        "CHILDRENS_EDUCATION" -> listOf(
            Color(0xFFFFFDF5), // Light beige base
            Color(0xFFF5F9FF), // Slightly cool beige
            Color(0xFFEBF3FF)  // Light blue-beige
        )
        "VACATION" -> listOf(
            Color(0xFFFFFDF5), // Light beige base
            Color(0xFFFDF5FF), // Slightly purple-beige
            Color(0xFFF8EBFF)  // Light lavender-beige
        )
        "SAVINGS" -> listOf(
            Color(0xFFFFFDF5), // Light beige base
            Color(0xFFF5FFF5), // Slightly green-beige
            Color(0xFFEBFFEB)  // Light mint-beige
        )
        "SAVINGS_PLUS" -> listOf(
            Color(0xFFE8F5E9), // Very light emerald
            Color(0xFFC8E6C9), // Light emerald
            Color(0xFFA5D6A7)  // Soft emerald
        )
        "GLOBAL_EXPOSURE" -> listOf(
            Color(0xFFE0F2F1),
            Color(0xFFE1F5FE),
            Color(0xFFF3E5F5)
        )
        "ALL_IN_ONE" -> listOf(
            Color(0xFF95A5CD), // Muted blue
            Color(0xFFF0F8FF), // AliceBlue
            Color(0xFFF0FFF0), // HoneyDew
            Color(0xFFFFF8DC), // Cornsilk
            Color(0xFFE6E6FA)  // Lavender
        )
        else -> listOf(
            Color.White,
            Color.White
        )
    }
}

fun getIconBackgroundColorForCategory(category: String?, colorTheme: String?): Color {
    return when (category?.uppercase()) {
        "GOLD" -> Color(0xFFFFF4D6) // Rich golden yellow
        "SILVER" -> Color(0xFFF5F5F5) // Light silver/gray
        "FESTIVAL_SPENDS" -> Color(0xFFFFE8E8) // Light rose
        "CHILDRENS_EDUCATION" -> Color(0xFFBBDEFB) // Light blue
        "VACATION" -> Color(0xFFE1BEE7) // Light purple
        "SAVINGS" -> Color(0xFFC8E6C9) // Light green
        "SAVINGS_PLUS" -> Color(0xFFA5D6A7) // Emerald tint
        "GLOBAL_EXPOSURE" -> Color(0xFFB2DFDB) // Light teal
        "ALL_IN_ONE" -> Color(0xFFD4DBEB) // Light blue-gray
        else -> Color(0xFFC8E6C9) // Default light green
    }
}

fun getGoalIconDrawable(category: String?): DrawableResource? {
    return when (category?.uppercase()) {
        "GOLD" -> Res.drawable.gold_icon
        "SILVER" -> Res.drawable.silver_icon
        "FESTIVAL_SPENDS" -> Res.drawable.festivals_icon
        "SAVINGS" -> Res.drawable.savings_icon
        "SAVINGS_PLUS" -> Res.drawable.savings_plus
        else -> null
    }
}

fun getFundLogo(fundName: String?): DrawableResource {
    val name = fundName?.lowercase() ?: return Res.drawable.axis_lo
    return when {
        name.contains("axis") -> Res.drawable.axis_lo
        name.contains("aditya") -> Res.drawable.aditya
        name.contains("invesco") -> Res.drawable.invesco
        name.contains("nippon") -> Res.drawable.nippon
        else -> Res.drawable.axis_lo
    }
}

fun getCorrelationText(category: String?): String {
    return when (category?.uppercase()) {
        "GOLD" -> "Grows in line with gold price"
        "SILVER" -> "Grows in line with silver price"
        "FESTIVAL_SPENDS" -> "Grows with market performance"
        "CHILDRENS_EDUCATION" -> "Grows with market performance"
        "VACATION" -> "Grows with market performance"
        "SAVINGS" -> "Expected growth up to 7%"
        "SAVINGS_PLUS" -> "Expected growth up to 7% • Instant Redeem"
        "GLOBAL_EXPOSURE" -> "Grows with global market performance"
        "ALL_IN_ONE" -> "Diversified growth across asset classes"
        else -> "Grows with market performance"
    }
}

fun getInvestmentTypeText(category: String?): String {
    return when (category?.uppercase()) {
        "GOLD" -> "Gold ETFs"
        "SILVER" -> "Silver ETFs"
        "FESTIVAL_SPENDS" -> "Equity Funds"
        "CHILDRENS_EDUCATION" -> "Equity Funds"
        "VACATION" -> "Equity Funds"
        "SAVINGS" -> "Equity Funds"
        "GLOBAL_EXPOSURE" -> "International Equity Funds"
        "ALL_IN_ONE" -> "Diversified Portfolio"
        else -> "Equity Funds"
    }
}

fun String.toColor(): Color {
    val raw = this.trim()
    val lower = raw.lowercase()
    val cleanedHex = lower.removePrefix("#").removePrefix("0x")
    if (cleanedHex.length == 6 || cleanedHex.length == 8) {
        runCatching {
            val argb = if (cleanedHex.length == 6) "ff$cleanedHex" else cleanedHex
            return Color(argb.toLong(16))
        }
    }

    return when (lower) {
        "green" -> V2SuccessGreen
        "orange" -> Color(0xFFFF9800)
        "yellow" -> Color(0xFFFFEB3B)
        "gold", "ffd700", "d4af37", "daa520" -> Color(0xFFDAA520)
        "silver", "c4c4c4", "ababab" -> Color(0xFFababab)
        "red" -> Color(0xFFF44336)
        "blue" -> Color(0xFF2196F3)
        "purple" -> Color(0xFF9C27B0)
        "teal" -> Color(0xFF009688)
        else -> V2SuccessGreen
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

fun formatNextSipDate(dateString: String?): String? {
    if (dateString.isNullOrBlank()) return null
    return try {
        // Simple manual format for KMP to avoid SimpleDateFormat complexity
        val datePart = dateString.split("T").first()
        val parts = datePart.split("-")
        if (parts.size == 3) {
            val day = parts[2]
            val month = when (parts[1]) {
                "01" -> "Jan"
                "02" -> "Feb"
                "03" -> "Mar"
                "04" -> "Apr"
                "05" -> "May"
                "06" -> "Jun"
                "07" -> "Jul"
                "08" -> "Aug"
                "09" -> "Sep"
                "10" -> "Oct"
                "11" -> "Nov"
                "12" -> "Dec"
                else -> parts[1]
            }
            "$day $month"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}


