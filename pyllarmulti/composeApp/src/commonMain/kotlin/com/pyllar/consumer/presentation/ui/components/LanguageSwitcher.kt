package com.pyllar.consumer.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.compositionLocalOf
import com.pyllar.consumer.getPlatform

/** Optional saver for language preference (e.g. to local DB). Provided by the root so LanguageLetterButton can persist when user changes language on any screen. */
val LocalLanguagePreferenceSaver = compositionLocalOf<((String) -> Unit)?> { null }

/** Language tag to single-letter (native script) for the language button. */
private val LANGUAGE_LETTERS = mapOf(
    "en" to "A",
    "hi" to "अ",
    "ta" to "அ",
    "ml" to "അ",
    "kn" to "ಅ",
    "te" to "అ"
)

private val LANGUAGE_TAGS = listOf("en", "hi", "ta", "ml", "kn", "te")

/** 
 * For KMP, we mock the localized names. In a full implementation, you would use composeResources 
 * stringResource() with appropriate locale settings, or a custom translation map. 
 */
private val LANGUAGE_NAMES = listOf(
    "English",
    "हिंदी",
    "தமிழ்",
    "മലയാളം",
    "ಕನ್ನಡ",
    "తెలుగు"
)

/**
 * Returns the single letter to display for the current app locale.
 * English -> A, Tamil -> அ, Hindi -> अ, etc.
 */
@Composable
fun currentLanguageLetter(currentLocaleTag: String = "en"): String {
    return LANGUAGE_LETTERS[currentLocaleTag] ?: "A"
}

/**
 * Single-letter button showing current app language. Click opens language picker dialog.
 * Place next to the Help button on screens.
 *
 * @param currentLanguageTag The current language tag ("en", "hi", etc.)
 * @param textColor Use MaterialTheme.colorScheme.primary for light backgrounds,
 *   MaterialTheme.colorScheme.onPrimary for dark headers.
 * @param onLanguageChange Callback when user selects a language.
 */
@Composable
fun LanguageLetterButton(
    modifier: Modifier = Modifier,
    currentLanguageTag: String = "en",
    textColor: Color = MaterialTheme.colorScheme.primary,
    onLanguageChange: ((String) -> Unit)? = null
) {
    if (remember { getPlatform().name.contains("iOS", ignoreCase = true) }) {
        return
    }

    var showDialog by remember { mutableStateOf(false) }
    val letter = currentLanguageLetter(currentLanguageTag)

    TextButton(
        onClick = { showDialog = true },
        modifier = modifier
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = textColor
        )
    }

    if (showDialog) {
        val persistLanguage = LocalLanguagePreferenceSaver.current
        LanguagePickerDialog(
            onDismiss = { showDialog = false },
            onLanguageSelected = { languageTag ->
                onLanguageChange?.invoke(languageTag)
                persistLanguage?.invoke(languageTag)
                showDialog = false
            }
        )
    }
}

/**
 * Host to show the language picker dialog from outside (e.g. from a menu).
 * Use when you need to open the language picker programmatically.
 */
@Composable
fun LanguagePickerDialogHost(
    show: Boolean,
    onDismiss: () -> Unit,
    onLanguageChange: ((String) -> Unit)? = null
) {
    if (!show) return
    val persistLanguage = LocalLanguagePreferenceSaver.current
    LanguagePickerDialog(
        onDismiss = onDismiss,
        onLanguageSelected = { languageTag ->
            onLanguageChange?.invoke(languageTag)
            persistLanguage?.invoke(languageTag)
            onDismiss()
        }
    )
}

/**
 * Dialog listing all app languages. On item click, applies that locale and dismisses.
 */
@Composable
internal fun LanguagePickerDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (languageTag: String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose Preferred Language", // Hardcoded fallback for KMP initially
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                LANGUAGE_TAGS.forEachIndexed { index, tag ->
                    val name = LANGUAGE_NAMES.getOrElse(index) { tag }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onLanguageSelected(tag)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
