package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.domain.storage.SessionStore
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.*

private val DsObsidian = Color(0xFF0A2415)
private val DsInk = Color(0xFF3E2723)
private val DsInkSoft = Color(0xFF6D4C41)
private val DsCream = Color(0xFFFBF9F4)
private val DsGold = Color(0xFFD4AF37)
private val DsGoldDeep = Color(0xFF8B6B25)
private val DsSubtleBorder = Color(0xFFEFEBE9)
private val DsSuccessGreen = Color(0xFF2E7D32)

private enum class DoubtsSurveyStep { SURVEY, ANSWER, CALLBACK, FREE_TEXT, THANKS }

private data class DoubtEntry(
    val key: String,
    val emoji: String,
    val label: String,
    /** (text, isBold) segments — mirrors the inline strong spans in source copy. */
    val answer: List<Pair<String, Boolean>>,
    val detail: List<String>
)

@Composable
private fun rememberDoubtEntries(): List<DoubtEntry> {
    return listOf(
        DoubtEntry(
            key = "safety",
            emoji = "🛡️",
            label = stringResource(Res.string.doubts_safety_label),
            answer = listOf(
                stringResource(Res.string.doubts_safety_ans_1) to false,
                stringResource(Res.string.doubts_safety_ans_2) to true,
                stringResource(Res.string.doubts_safety_ans_3) to false
            ),
            detail = listOf(
                stringResource(Res.string.doubts_safety_detail_1),
                stringResource(Res.string.doubts_safety_detail_2)
            )
        ),
        DoubtEntry(
            key = "howworks",
            emoji = "❓",
            label = stringResource(Res.string.doubts_howworks_label),
            answer = listOf(
                stringResource(Res.string.doubts_howworks_ans_1) to false
            ),
            detail = listOf(
                stringResource(Res.string.doubts_howworks_detail_1),
                stringResource(Res.string.doubts_howworks_detail_2)
            )
        ),
        DoubtEntry(
            key = "howmuch",
            emoji = "💰",
            label = stringResource(Res.string.doubts_howmuch_label),
            answer = listOf(
                stringResource(Res.string.doubts_howmuch_ans_1) to false
            ),
            detail = listOf(
                stringResource(Res.string.doubts_howmuch_detail_1),
                stringResource(Res.string.doubts_howmuch_detail_2)
            )
        ),
        DoubtEntry(
            key = "withdraw",
            emoji = "🔓",
            label = stringResource(Res.string.doubts_withdraw_label),
            answer = listOf(
                stringResource(Res.string.doubts_withdraw_ans_1) to false
            ),
            detail = listOf(
                stringResource(Res.string.doubts_withdraw_detail_1),
                stringResource(Res.string.doubts_withdraw_detail_2)
            )
        )
    )
}

private const val SCREEN_NAME = "SipAmountV3"

/**
 * Exit-intent "doubts survey" bottom sheet — shown once when the user tries to leave
 * [SipAmountScreenV3] on the 5th+ visit without having started a SIP. Offers a short FAQ
 * picker, an inline answer, a free-text option, and a real callback request, each of which
 * is recorded via [DoubtsSurveyViewModel] for product/support follow-up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubtsSurveyBottomSheet(
    goalId: String,
    onDismiss: () -> Unit,
    viewModel: DoubtsSurveyViewModel = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    val doubtEntries = rememberDoubtEntries()

    var step by remember { mutableStateOf(DoubtsSurveyStep.SURVEY) }
    var selectedDoubt by remember { mutableStateOf<DoubtEntry?>(null) }
    var freeTextValue by remember { mutableStateOf("") }
    var thanksTitle by remember { mutableStateOf("") }
    var thanksSubtitle by remember { mutableStateOf("") }
    var thanksIcon by remember { mutableStateOf("✓") }

    val defaultMaskedPhone = stringResource(Res.string.doubts_masked_phone_default)
    var maskedPhone by remember(defaultMaskedPhone) { mutableStateOf(defaultMaskedPhone) }

    val thanksSkipTitle = stringResource(Res.string.doubts_thanks_skip_title)
    val thanksSkipSubtitle = stringResource(Res.string.doubts_thanks_skip_subtitle)
    val thanksHelpedTitle = stringResource(Res.string.doubts_thanks_helped_title)
    val thanksHelpedSubtitle = stringResource(Res.string.doubts_thanks_helped_subtitle)
    val thanksCallbackTitle = stringResource(Res.string.doubts_thanks_callback_title)
    val thanksCallbackSubtitle = stringResource(Res.string.doubts_thanks_callback_subtitle)
    val thanksFreetextTitle = stringResource(Res.string.doubts_thanks_freetext_title)

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logEvent("doubts_survey_shown", mapOf("goal_id" to goalId))
        val phone = sessionStore.getValue(KeyValueConstants.PHONE).orEmpty()
        if (phone.length > 4) {
            maskedPhone = "+91 •••••${phone.takeLast(4)}"
        }
    }

    fun submitAndShowThanks(
        selectedOption: String,
        freeText: String? = null,
        requestCallback: Boolean = false,
        title: String,
        subtitle: String,
        icon: String
    ) {
        viewModel.submit(
            screenName = SCREEN_NAME,
            goalId = goalId,
            selectedOption = selectedOption,
            freeText = freeText,
            requestCallback = requestCallback
        )
        thanksTitle = title
        thanksSubtitle = subtitle
        thanksIcon = icon
        step = DoubtsSurveyStep.THANKS
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            when (step) {
                DoubtsSurveyStep.SURVEY -> SurveyStepContent(
                    doubtEntries = doubtEntries,
                    onDoubtSelected = { doubt ->
                        selectedDoubt = doubt
                        step = DoubtsSurveyStep.ANSWER
                    },
                    onSomethingElse = { step = DoubtsSurveyStep.FREE_TEXT },
                    onSkip = {
                        submitAndShowThanks(
                            selectedOption = "skip",
                            title = thanksSkipTitle,
                            subtitle = thanksSkipSubtitle,
                            icon = "👍"
                        )
                    }
                )

                DoubtsSurveyStep.ANSWER -> selectedDoubt?.let { doubt ->
                    AnswerStepContent(
                        doubt = doubt,
                        onThatHelps = {
                            submitAndShowThanks(
                                selectedOption = doubt.key,
                                title = thanksHelpedTitle,
                                subtitle = thanksHelpedSubtitle,
                                icon = "✓"
                            )
                        },
                        onRequestCallback = { step = DoubtsSurveyStep.CALLBACK }
                    )
                }

                DoubtsSurveyStep.CALLBACK -> CallbackStepContent(
                    maskedPhone = maskedPhone,
                    onConfirm = {
                        submitAndShowThanks(
                            selectedOption = selectedDoubt?.key ?: "callback",
                            requestCallback = true,
                            title = thanksCallbackTitle,
                            subtitle = thanksCallbackSubtitle,
                            icon = "📞"
                        )
                    },
                    onNevermind = onDismiss
                )

                DoubtsSurveyStep.FREE_TEXT -> FreeTextStepContent(
                    value = freeTextValue,
                    onValueChange = { freeTextValue = it },
                    onSend = {
                        submitAndShowThanks(
                            selectedOption = "other",
                            freeText = freeTextValue,
                            title = thanksFreetextTitle,
                            subtitle = thanksHelpedSubtitle,
                            icon = "✓"
                        )
                    }
                )

                DoubtsSurveyStep.THANKS -> ThanksStepContent(
                    icon = thanksIcon,
                    title = thanksTitle,
                    subtitle = thanksSubtitle,
                    onContinue = onDismiss
                )
            }
        }
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 16.dp)
            .size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(DsSubtleBorder)
    )
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        color = DsGoldDeep
    )
}

@Composable
private fun SurveyStepContent(
    doubtEntries: List<DoubtEntry>,
    onDoubtSelected: (DoubtEntry) -> Unit,
    onSomethingElse: () -> Unit,
    onSkip: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        SheetDragHandle()
    }
    Eyebrow(stringResource(Res.string.doubts_eyebrow_quick_question))
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(Res.string.doubts_survey_title),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = DsInk,
        lineHeight = 23.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(Res.string.doubts_survey_subtitle),
        fontSize = 13.sp,
        color = DsInkSoft
    )
    Spacer(modifier = Modifier.height(16.dp))

    doubtEntries.forEach { doubt ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DsCream)
                .border(1.dp, DsSubtleBorder, RoundedCornerShape(12.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onDoubtSelected(doubt)
                }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = doubt.emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = doubt.label, fontSize = 14.sp, color = DsInk)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DsSubtleBorder, RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onSomethingElse()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(Res.string.doubts_something_else),
            fontSize = 13.sp,
            color = DsInkSoft
        )
    }

    Text(
        text = stringResource(Res.string.doubts_just_browsing),
        fontSize = 13.sp,
        color = DsInkSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSkip() }
    )
}

@Composable
private fun AnswerStepContent(
    doubt: DoubtEntry,
    onThatHelps: () -> Unit,
    onRequestCallback: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        SheetDragHandle()
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(DsGold.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "${doubt.emoji} ${stringResource(Res.string.doubts_you_asked)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = DsGoldDeep
        )
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(text = doubt.label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DsInk, lineHeight = 23.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = buildAnnotatedString {
            doubt.answer.forEach { (segment, isBold) ->
                if (isBold) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = DsGoldDeep)) { append(segment) }
                } else {
                    append(segment)
                }
            }
        },
        fontSize = 15.sp,
        lineHeight = 23.sp,
        color = DsInk
    )
    Spacer(modifier = Modifier.height(14.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DsCream)
            .border(1.dp, DsSubtleBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        doubt.detail.forEach { line ->
            Row(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(text = "✓", color = DsSuccessGreen, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = line, fontSize = 13.sp, color = DsInkSoft, lineHeight = 19.sp)
            }
        }
    }
    Spacer(modifier = Modifier.height(18.dp))
    Button(
        onClick = onThatHelps,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DsObsidian, contentColor = DsCream)
    ) {
        Text(text = stringResource(Res.string.doubts_that_helps), fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DsGold.copy(alpha = 0.10f))
            .border(1.dp, DsGoldDeep, RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRequestCallback() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "📞", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.doubts_request_callback),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DsGoldDeep
        )
    }
}

@Composable
private fun CallbackStepContent(
    maskedPhone: String,
    onConfirm: () -> Unit,
    onNevermind: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        SheetDragHandle()
    }
    Eyebrow(stringResource(Res.string.doubts_eyebrow_one_on_one))
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = stringResource(Res.string.doubts_callback_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DsInk)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(Res.string.doubts_callback_subtitle),
        fontSize = 13.sp,
        color = DsInkSoft,
        lineHeight = 19.sp
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DsCream)
            .border(1.dp, DsSubtleBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "📞", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = maskedPhone, fontSize = 14.sp, color = DsInk)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DsObsidian, contentColor = DsCream)
    ) {
        Text(text = stringResource(Res.string.doubts_callback_confirm), fontWeight = FontWeight.Bold)
    }
    Text(
        text = stringResource(Res.string.doubts_callback_nevermind),
        fontSize = 13.sp,
        color = DsInkSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onNevermind() }
    )
}

@Composable
private fun FreeTextStepContent(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        SheetDragHandle()
    }
    Eyebrow(stringResource(Res.string.doubts_eyebrow_own_words))
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = stringResource(Res.string.doubts_freetext_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DsInk)
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(Res.string.doubts_freetext_placeholder)) },
        minLines = 4,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DsGold,
            unfocusedBorderColor = DsSubtleBorder
        )
    )
    Spacer(modifier = Modifier.height(14.dp))
    Button(
        onClick = onSend,
        enabled = value.isNotBlank(),
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DsObsidian, contentColor = DsCream)
    ) {
        Text(text = stringResource(Res.string.doubts_send), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThanksStepContent(
    icon: String,
    title: String,
    subtitle: String,
    onContinue: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        SheetDragHandle()
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DsGold),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DsObsidian)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = DsInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = DsInkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DsObsidian, contentColor = DsCream)
        ) {
            Text(text = stringResource(Res.string.doubts_thanks_continue), fontWeight = FontWeight.Bold)
        }
    }
}
