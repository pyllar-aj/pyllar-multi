package com.pyllar.consumer.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.outfit_regular
import pyllar.composeapp.generated.resources.outfit_medium
import pyllar.composeapp.generated.resources.outfit_semibold
import pyllar.composeapp.generated.resources.outfit_bold
import pyllar.composeapp.generated.resources.cursive_font

// Outfit Font Family
@Composable
fun getOutfitFontFamily() = FontFamily(
    Font(Res.font.outfit_regular, FontWeight.Normal),
    Font(Res.font.outfit_medium, FontWeight.Medium),
    Font(Res.font.outfit_semibold, FontWeight.SemiBold),
    Font(Res.font.outfit_bold, FontWeight.Bold)
)

// Cursive Font Family for special accents
@Composable
fun getCursiveFontFamily() = FontFamily(
    Font(Res.font.cursive_font, FontWeight.Normal)
)

@Composable
fun AppTypography() = Typography(
    bodyLarge = TextStyle(
        fontFamily = getOutfitFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = getOutfitFontFamily(),
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = getOutfitFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
