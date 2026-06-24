package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.data.remote.model.dto.UserDetailsFetchState
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.ui.theme.getOutfitFontFamily
import com.pyllar.consumer.util.BackHandler
import io.github.alexzhirkevich.compottie.LottieAnimation
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.LottieConstants
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.*
import org.koin.compose.koinInject

private val PDObsidian = Color(0xFF0A2415)
private val PDGold = Color(0xFFD4AF37)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun PennyDropLoadingScreen(
    userId: String = "",
    onBack: () -> Unit = {},
    onComplete: (String) -> Unit = {},
    viewModel: PennyDropLoadingModel = koinInject(),
    sessionStore: SessionStore = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var resolvedUserId by remember { mutableStateOf("") }
    var resolvedPhoneNumber by remember { mutableStateOf("") }
    var resolvedName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        resolvedUserId = userId.ifBlank { sessionStore.getCurrentUserId() }
        resolvedPhoneNumber = sessionStore.getCurrentPhone()
        resolvedName = sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.FULL_NAME) ?: ""
        viewModel.start(resolvedUserId, resolvedPhoneNumber, resolvedName)
    }

    LaunchedEffect(uiState.overallStatus) {
        if (uiState.overallStatus == UserDetailsFetchState.SUCCESS) {
            delay(1200L)
            onComplete(uiState.nextScreen ?: ScreenNames.PRE_VERIFICATION)
        } else if (uiState.overallStatus == UserDetailsFetchState.FAILED) {
            delay(1800L)
            onComplete(uiState.nextScreen ?: ScreenNames.PRE_VERIFICATION)
        }
    }

    val stageIndex = stageIndexFor(uiState)
    val stageLabels = listOf(
        stringResource(Res.string.penny_drop_stage_initiated),
        stringResource(Res.string.penny_drop_stage_sent),
        stringResource(Res.string.penny_drop_stage_matched),
        stringResource(Res.string.penny_drop_stage_done)
    )

    BackHandler { onBack() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PDObsidian)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Back nav
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White.copy(alpha = 0.55f)
                    )
                }
                Text(
                    text = stringResource(Res.string.penny_drop_header_label),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = getOutfitFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.3.sp
                    ),
                    color = PDGold
                )
            }

            // Pulse center
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.overallStatus == UserDetailsFetchState.FAILED) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(Res.string.penny_drop_error_generic),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = getOutfitFontFamily(),
                                fontSize = 14.sp
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(Res.string.penny_drop_continuing_anyway),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = getOutfitFontFamily(),
                                fontSize = 11.sp
                            ),
                            color = PDGold.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    PulseRings(accentColor = PDGold)

                    BreathingContent {
                        val composition by rememberLottieComposition {
                            val json = Res.readBytes("files/secure.json").decodeToString()
                            LottieCompositionSpec.JsonString(json)
                        }
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(96.dp)
                        )

                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(170.dp)
                        ) {
                            Crossfade(targetState = stageIndex, label = "pennyDropStatus") { idx ->
                                Text(
                                    text = stringResource(statusTextResFor(idx)),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        letterSpacing = 1.1.sp
                                    ),
                                    color = PDGold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Text(
                            text = stringResource(Res.string.penny_drop_please_wait),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = getOutfitFontFamily(),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            // Headline copy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.penny_drop_headline).replace("\\n", "\n"),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = getOutfitFontFamily(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.4).sp,
                        lineHeight = 29.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(Res.string.penny_drop_subtitle).replace("\\n", "\n"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = getOutfitFontFamily(),
                        fontSize = 13.sp,
                        lineHeight = 20.8.sp
                    ),
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // 4-dot progress track
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 22.dp),
                verticalAlignment = Alignment.Top
            ) {
                stageLabels.forEachIndexed { index, label ->
                    ProgressStageDot(
                        label = label,
                        isActive = index == stageIndex,
                        accentColor = PDGold,
                        modifier = Modifier.weight(1f)
                    )
                    if (index != stageLabels.lastIndex) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .padding(top = 5.dp)
                                .background(PDGold.copy(alpha = 0.16f))
                        )
                    }
                }
            }

            // Trust strip
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PDGold.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.penny_drop_trust_secured),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = PDGold.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(Res.string.penny_drop_trust_amount),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = PDGold.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun stageIndexFor(state: PennyDropUiState): Int = when {
    state.overallStatus == UserDetailsFetchState.SUCCESS -> 3
    state.mobileAccountStatus == UserDetailsFetchState.SUCCESS && state.creditBureauStatus != UserDetailsFetchState.SUCCESS -> 2
    state.mobileAccountStatus == UserDetailsFetchState.IN_PROGRESS -> 1
    else -> 0
}

private fun statusTextResFor(stageIndex: Int): org.jetbrains.compose.resources.StringResource = when (stageIndex) {
    1 -> Res.string.penny_drop_status_sending
    2 -> Res.string.penny_drop_status_name_check
    3 -> Res.string.penny_drop_status_verified
    else -> Res.string.penny_drop_status_initiating
}

@Composable
private fun BreathingContent(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "breathe")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )
    Column(
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun PulseRings(accentColor: Color) {
    Box(contentAlignment = Alignment.Center) {
        // Ambient radial glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accentColor.copy(alpha = 0.09f), Color.Transparent)
                    )
                )
        )

        // 3 staggered pulse rings
        repeat(3) { index ->
            PulseRing(accentColor = accentColor, startOffsetMillis = index * 800)
        }

        // Inner static ring
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.055f))
                .border(1.dp, accentColor.copy(alpha = 0.22f), CircleShape)
        )
    }
}

@Composable
private fun PulseRing(accentColor: Color, startOffsetMillis: Int) {
    val transition = rememberInfiniteTransition(label = "pulseRing")
    val pulseEasing = CubicBezierEasing(0.15f, 0f, 0.1f, 1f)
    val scale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 2.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = pulseEasing),
            initialStartOffset = StartOffset(startOffsetMillis)
        ),
        label = "pulseScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = pulseEasing),
            initialStartOffset = StartOffset(startOffsetMillis)
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(190.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .border(1.5.dp, accentColor.copy(alpha = 0.52f), CircleShape)
    )
}

@Composable
private fun ProgressStageDot(
    label: String,
    isActive: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isActive) accentColor else accentColor.copy(alpha = 0.18f))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 0.4.sp),
            color = Color.White.copy(alpha = 0.3f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}
