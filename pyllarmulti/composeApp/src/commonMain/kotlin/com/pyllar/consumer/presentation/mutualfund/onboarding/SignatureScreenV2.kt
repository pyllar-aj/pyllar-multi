package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.platform.PlatformImage
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.pyllar.consumer.util.toUserFriendlyErrorMessage
import pyllar.composeapp.generated.resources.*

// ── V2 visual language: cream surface, dark bronze ink, luxury gold accents ──
private val SGV2Cream = Color(0xFFFBF9F4)
private val SGV2CreamTint = Color(0xFFF5EEDB)
private val SGV2BronzeInk = Color(0xFF3E2723)
private val SGV2BronzeMuted = Color(0xFF6D4C41)
private val SGV2GoldDeep = Color(0xFF8B6B25)
private val SGV2GoldAccent = Color(0xFFD4AF37)
private val SGV2Obsidian = Color(0xFF0A2415)
private val SGV2LinkGreen = Color(0xFF1A7A42)
private val SGV2FieldBorder = Color(0xFFD7CCC8)
private val SGV2CardBorder = Color(0xFFEFEBE9)
private val SGV2InfoBorder = Color(0x428B6B25)
private val SGV2CanvasBorder = Color(0x668B6B25)
private val SGV2PlaceholderGold = Color(0x738B6B25)

@Composable
fun SignatureScreenV2(
    userId: String,
    kycAttemptId: String,
    investorId: String,
    onSignatureCompleted: (nextScreen: String?, redirectUrl: String?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: SignatureViewModel = koinInject(),
    sessionStore: com.pyllar.consumer.domain.storage.SessionStore = koinInject()
) {
    var signaturePaths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var hasSignature by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var signatureBoxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("SignatureV4")
    }

    val digitalSignatureText = stringResource(Res.string.digital_signature)
    val signatureDescriptionText = stringResource(Res.string.signature_description)
    val clearText = stringResource(Res.string.clear)
    val processingText = stringResource(Res.string.processing)
    val continueText = stringResource(Res.string.btn_continue)
    val connectionFailedText = stringResource(Res.string.connection_failed_try_again)
    val failedToSaveSignatureText = stringResource(Res.string.failed_to_save_signature_try_again)

    // Stepper logic: completedStep = 4 if completed, else 3 — same contract as V1 SignatureScreen
    val completedStep = if (hasSignature) 4 else 3

    Box(modifier = Modifier.fillMaxSize().background(SGV2Cream)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageLetterButton(textColor = SGV2LinkGreen)
                    TextButton(onClick = onNavigateToHelp) {
                        Text(stringResource(Res.string.help), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SGV2LinkGreen)
                    }
                }
            }

            Surface(color = SGV2Cream, shadowElevation = 8.dp, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                OnboardingStepper(currentStep = 2, completedStep = completedStep, currentScreenRoute = ScreenNames.SIGNATURE)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 28.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = digitalSignatureText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SGV2BronzeInk)
                Text(text = signatureDescriptionText, fontSize = 11.sp, color = SGV2BronzeMuted, lineHeight = 17.sp)

                // ── E-sign info card ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(SGV2Cream, SGV2CreamTint)), RoundedCornerShape(14.dp))
                        .border(1.dp, SGV2InfoBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(SGV2Obsidian, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = SGV2GoldAccent, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(stringResource(Res.string.esign_will_follow_title), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SGV2BronzeInk)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(stringResource(Res.string.esign_will_follow_description), fontSize = 11.sp, color = SGV2BronzeMuted, lineHeight = 16.sp)
                    }
                }

                // ── Signature canvas card ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, SGV2CardBorder, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Text(stringResource(Res.string.your_signature_label), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.13.em, color = SGV2GoldAccent)
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .border(width = 2.dp, color = SGV2CanvasBorder, shape = RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .semantics { testTag = "signature_canvas" }
                            .onGloballyPositioned { coordinates ->
                                signatureBoxCoordinates = coordinates
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath = Path().apply {
                                            moveTo(offset.x, offset.y)
                                        }
                                        hasSignature = true
                                    },
                                    onDragEnd = {
                                        currentPath?.let { path ->
                                            signaturePaths = signaturePaths + path
                                            currentPath = null
                                        }
                                    }
                                ) { change, _ ->
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    // We need to force recompose since Path is not observable
                                    val p = currentPath
                                    currentPath = null
                                    currentPath = p
                                }
                            }
                    ) {
                        if (!hasSignature) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = SGV2PlaceholderGold, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(Res.string.sign_here_placeholder), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SGV2PlaceholderGold)
                            }
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            signaturePaths.forEach { path ->
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = Stroke(
                                        width = 5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                            currentPath?.let { path ->
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = Stroke(
                                        width = 5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }

                    if (hasSignature) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(Color.White, RoundedCornerShape(50))
                                .border(1.5.dp, SGV2FieldBorder, RoundedCornerShape(50))
                                .clickable {
                                    signaturePaths = emptyList()
                                    currentPath = null
                                    hasSignature = false
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = clearText, tint = SGV2BronzeMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(clearText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SGV2BronzeMuted)
                        }
                    }
                }

                // ── Consent card ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(SGV2Cream, SGV2CreamTint)), RoundedCornerShape(12.dp))
                        .border(1.dp, SGV2InfoBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = SGV2LinkGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.signature_consent_note), fontSize = 11.sp, color = SGV2BronzeMuted, lineHeight = 17.sp)
                }

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x14C62828), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = errorMessage!!, fontSize = 12.sp, color = Color(0xFFC62828), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }

                // ── CTA ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (hasSignature) Brush.linearGradient(listOf(SGV2GoldAccent, SGV2GoldDeep)) else Brush.linearGradient(listOf(SGV2FieldBorder, SGV2FieldBorder)),
                            RoundedCornerShape(50)
                        )
                        .padding(1.5.dp)
                ) {
                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            val coordinates = signatureBoxCoordinates ?: return@Button
                            isLoading = true
                            scope.launch {
                                errorMessage = null
                                try {
                                    val bytes = PlatformImage.captureToPng(coordinates)
                                    if (bytes != null) {
                                        PlatformAnalyticsLogger.logEvent(
                                            "signature_submit_attempt",
                                            mapOf("signature_present" to true, "screen_version" to "v4")
                                        )
                                        val effectiveKycAttemptId = if (kycAttemptId.isBlank()) {
                                            sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                                        } else kycAttemptId

                                        viewModel.uploadSignatureFile(bytes, effectiveKycAttemptId).collect { result ->
                                            when (result) {
                                                is Resource.Loading -> {
                                                    // Loading state
                                                }
                                                is Resource.Success -> {
                                                    PlatformAnalyticsLogger.logEvent("signature_submit_success", mapOf("screen_version" to "v4"))
                                                    isLoading = false
                                                    val redirectUrl = result.data?.redirectUrl
                                                        ?: result.navigation?.getParam("redirect_url")
                                                        ?: result.navigation?.getParam("esign_url")
                                                    onSignatureCompleted(result.navigation?.nextScreen, redirectUrl)
                                                }
                                                is Resource.Error -> {
                                                    isLoading = false
                                                    errorMessage = (result.message ?: connectionFailedText).toUserFriendlyErrorMessage()
                                                    PlatformAnalyticsLogger.logEvent("signature_submit_error", mapOf("message" to (result.message ?: "unknown"), "screen_version" to "v4"))
                                                }
                                            }
                                        }
                                    } else {
                                        errorMessage = failedToSaveSignatureText
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    errorMessage = connectionFailedText
                                    isLoading = false
                                }
                            }
                        },
                        enabled = hasSignature && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SGV2Obsidian,
                            contentColor = SGV2Cream,
                            disabledContainerColor = SGV2CreamTint,
                            disabledContentColor = SGV2PlaceholderGold
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SGV2Cream, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(processingText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text(continueText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (hasSignature) {
                                Spacer(modifier = Modifier.width(7.dp))
                                Text("→", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SGV2GoldAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}
