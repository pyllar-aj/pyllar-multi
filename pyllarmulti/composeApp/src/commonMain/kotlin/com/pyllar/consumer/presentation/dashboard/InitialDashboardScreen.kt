package com.pyllar.consumer.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.navigation.AppRoutes
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*

// ─── DESIGN TOKENS ────────────────────────────────────────────────────────────
private val GreenDark       = Color(0xFF1A7A42)
private val GreenMid        = Color(0xFF22924E)
private val GreenAccent     = Color(0xFF43C17A)   // "big dreams." highlight colour
private val AppBackground   = Color(0xFFF4F6F8)
private val PrimaryText     = Color(0xFF1C1C1E)
private val SecondaryText   = Color(0xFF5F5F5F)
private val LabelText       = Color(0xFF6B7280)

// Card colour palettes
private val GoldCardColors    = listOf(Color(0xFFF8EAB8), Color(0xFFEDD57A), Color(0xFFD4AF37))
private val SilverCardColors  = listOf(Color(0xFFECEEF1), Color(0xFFD6DCE8), Color(0xFFCBD2DD))
private val SavingsPlusCardColors = listOf(Color(0xFFF1F8E9), Color(0xFFDCEDC8), Color(0xFFC5E1A5))

private val GoldShimmerColors = listOf(
    Color(0xFFD4AF37).copy(alpha = 0.0f),
    Color(0xFFFFD700).copy(alpha = 0.9f),
    Color(0xFFFFF0A0).copy(alpha = 1.0f),
    Color(0xFFFFD700).copy(alpha = 0.9f),
    Color(0xFFD4AF37).copy(alpha = 0.0f),
)

private val SilverShimmerColors = listOf(
    Color(0xFFCBD2DD).copy(alpha = 0.0f),
    Color(0xFFE5E7EB).copy(alpha = 0.9f),
    Color(0xFFFFFFFF).copy(alpha = 1.0f),
    Color(0xFFE5E7EB).copy(alpha = 0.9f),
    Color(0xFFCBD2DD).copy(alpha = 0.0f),
)

// Hero banner – dark swirling green, radial light from top-right
private val BannerGradient = Brush.radialGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFF2DAF5E),
        0.5f to Color(0xFF1F8F4E),
        1.0f to Color(0xFF155C32)
    ),
    center = Offset(900f, -100f),
    radius = 1400f
)

// ─── SHIMMER ANIMATION (for gold card border) ─────────────────────────────────
@Composable
fun Modifier.shimmerBorder(
    colors: List<Color> = GoldShimmerColors,
    borderWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 20.dp
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_rotation"
    )

    return this.drawWithContent {
        drawContent()
        val strokePx = borderWidth.toPx()
        val cornerPx = cornerRadius.toPx()
        val rect = androidx.compose.ui.geometry.Rect(
            left = strokePx / 2f,
            top = strokePx / 2f,
            right = size.width - strokePx / 2f,
            bottom = size.height - strokePx / 2f
        )
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = rect,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx)
                )
            )
        }
        rotate(rotation, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawPath(
                path = path,
                brush = Brush.sweepGradient(colors),
                style = Stroke(width = strokePx)
            )
        }
    }
}

// ─── REUSABLE COMPONENTS ──────────────────────────────────────────────────────

@Composable
fun Modifier.premiumClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "press_scale")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
}

@Composable
private fun ChevronButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(25.dp)
            .shadow(0.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f))
            .premiumClickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.4f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = PrimaryText.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun InitialDashboardScreen(
    userId: String = "",
    onNavigateToOnboarding: (goalId: String, userId: String) -> Unit,
    onNavigateToRoute: (screen: String, preVerificationId: String?) -> Unit = { _, _ -> },
    onNavigateToHelp: () -> Unit = {}
) {
    val viewModel: InitialDashboardViewModel = koinInject()
    val platformActions: PlatformActions = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val goalsState by viewModel.goalsState.collectAsState()

    val allGoals = remember(goalsState.primaryGoals, goalsState.recommendedGoals) {
        (goalsState.primaryGoals + goalsState.recommendedGoals)
            .filter { it.category.uppercase() in listOf("GOLD", "SILVER", "SAVINGS_PLUS") }
            .sortedBy {
                when (it.category.uppercase()) {
                    "GOLD" -> 1
                    "SILVER" -> 2
                    "SAVINGS_PLUS" -> 3
                    else -> 4
                }
            }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("InitialDashboard")
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Spacer(modifier = Modifier.height(32.dp))
        // Share, Help at top right (fixed at top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share button
            IconButton(
                onClick = {
                    PlatformAnalyticsLogger.logEvent("share_app_clicked", mapOf("screen" to "InitialDashboard"))
                    platformActions.shareText("Build your wealth with Pyllar! https://pyllar.in", "Share Pyllar")
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = GreenMid,
                    modifier = Modifier.size(20.dp)
                )
            }

            TextButton(onClick = onNavigateToHelp) {
                Text(
                    text = "Help",
                    style = MaterialTheme.typography.labelLarge,
                    color = GreenMid
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    StartInvestingHeroCard()
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Start growing your wealth",
                            style = TextStyle(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryText,
                                letterSpacing = (-0.5).sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Invest & withdraw from the same bank account. Add more goals anytime.",
                            style = TextStyle(fontSize = 13.sp, color = SecondaryText.copy(alpha = 0.8f), lineHeight = 19.sp),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // Error
                if (errorMessage != null) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(errorMessage ?: "", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                if (goalsState.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GreenMid)
                        }
                    }
                } else {
                    items(allGoals.size) { idx ->
                        val goal = allGoals[idx]
                        InitialGoalCard(
                            goal = goal,
                            bucketData = goalsState.growthData[goal.goalId] ?: goalsState.growthData[goal.category.lowercase()],
                            onClick = {
                                if (isSubmitting) return@InitialGoalCard
                                if (userId.isBlank()) {
                                    errorMessage = "User ID is missing. Please log in again."
                                    return@InitialGoalCard
                                }
                                isSubmitting = true
                                errorMessage = null
                                coroutineScope.launch {
                                    try {
                                        val result = viewModel.selectGoal(userId = userId, goalId = goal.goalId)
                                        when (result) {
                                            is Resource.Success -> {
                                                val nextScreen = result.navigation?.nextScreen
                                                val userPurposeId = result.data?.userPurposeId
                                                
                                                if (!nextScreen.isNullOrBlank()) {
                                                    platformLog("🚀 Server-driven navigation to: $nextScreen (purposeId: $userPurposeId)")
                                                    onNavigateToRoute(nextScreen, userPurposeId)
                                                } else {
                                                    platformLog("⚠️ No nextScreen in response, falling back to default onboarding")
                                                    onNavigateToOnboarding(goal.goalId, userId)
                                                }
                                            }
                                            is Resource.Error -> {
                                                errorMessage = result.message ?: "Unable to select goal"
                                            }
                                            else -> Unit
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Something went wrong"
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }

            if (isSubmitting) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Color.White) }
            }
        }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun StartInvestingHeroCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF1A7A42).copy(alpha = 0.25f),
                spotColor = Color(0xFF1A7A42).copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BannerGradient)
                .drawBehind {
                    // Swirling texture lines
                    val w = size.width; val h = size.height
                    val path = Path()
                    for (i in 0..2) {
                        val yOff = h * (0.35f + i * 0.2f)
                        path.moveTo(0f, yOff)
                        path.cubicTo(w * 0.2f, yOff - 30f, w * 0.5f, yOff + 40f, w * 0.8f, yOff - 20f)
                        path.cubicTo(w * 0.9f, yOff - 30f, w, yOff + 10f, w, yOff)
                        drawPath(path, Color.White.copy(alpha = 0.06f - i * 0.015f), style = Stroke(1.5f))
                        path.reset()
                    }
                }
                .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // "START INVESTING" label (plain text, no pill)
                Text(
                    text = "START INVESTING",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Headline row – text + Gold bar image
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // "Small steps, big dreams." – "big dreams." in GreenAccent
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White)) { append("Small steps, ") }
                            withStyle(SpanStyle(color = GreenAccent)) { append("big dreams.") }
                        },
                        style = TextStyle(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Gold bar + rising chart (image)
                    Image(
                        painter = painterResource(Res.drawable.goldbar_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer(translationY = -8f, rotationZ = 5f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(1.dp))

                // Feature tag pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroPill(icon = Icons.Outlined.LocalOffer, text = stringResource(Res.string.tag_0_gst))
                    HeroPill(icon = Icons.Filled.LockOpen, text = stringResource(Res.string.no_lock_in))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row – frosted panel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    color = Color.White.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Row 1: Commodities
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatColumn(label = "Gold (2Y)", value = "+148%", modifier = Modifier.weight(1f))
                            Box(Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.15f)))
                            StatColumn(label = "Silver (2Y)", value = "+266.24%", modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        // Subtle horizontal divider
                        Box(
                            Modifier
                                .fillMaxWidth(0.6f)
                                .height(0.5.dp)
                                .background(Color.Gray.copy(alpha = 0.12f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Savings Yield (Full width for better readability)
                        StatColumn(
                            label = "Pyllar Savings goal earns",
                            value = "Up to 7% returns",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = TextStyle(fontSize = 11.sp, color = LabelText, fontWeight = FontWeight.Medium), textAlign = TextAlign.Center)
        Spacer(Modifier.height(3.dp))
        Text(text = value, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenDark), textAlign = TextAlign.Center)
    }
}

@Composable
private fun HeroPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.18f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            Text(text = text, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White))
        }
    }
}


// ── Goal card ─────────────────────────────────────────────────────────────────

@Composable
fun InitialGoalCard(
    goal: InvestmentGoal,
    bucketData: BucketGrowthData? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = goal.category.uppercase()

    val gradient = Brush.linearGradient(
        colors = when (category) {
            "GOLD"    -> GoldCardColors
            "SILVER"  -> SilverCardColors
            "SAVINGS", "SAVINGS_PLUS" -> SavingsPlusCardColors
            else      -> listOf(Color.White, Color.White)
        },
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val titleColor = when (category) {
        "GOLD"    -> Color(0xFFB8860B)
        "SILVER"  -> Color(0xFF3A3A3A)
        "SAVINGS", "SAVINGS_PLUS" -> GreenDark
        else      -> PrimaryText
    }

    val highlightColor = when (category) {
        "GOLD"    -> Color(0xFFB8860B)
        "SILVER"  -> Color(0xFF4A4A4A)
        else      -> GreenDark
    }

    // Base modifier with shadow
    var cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 7.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = Color.Black.copy(alpha = 0.10f),
            spotColor = Color.Black.copy(alpha = 0.08f)
        )
        .premiumClickable(onClick = onClick)

    // Gold & Silver cards gets shiny animated border
    when (category) {
        "GOLD" -> cardModifier = cardModifier.shimmerBorder(colors = GoldShimmerColors, cornerRadius = 20.dp)
        "SILVER" -> cardModifier = cardModifier.shimmerBorder(colors = SilverShimmerColors, cornerRadius = 20.dp)
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        border = if (category == "SILVER") BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)) else if (category == "SAVINGS_PLUS") BorderStroke(1.dp, GreenDark.copy(alpha = 0.4f)) else null
    ) {
        var showSavingsPlusInfo by remember { mutableStateOf(false) }
        Box(modifier = Modifier.background(gradient)) {

            // Silver shimmer highlight at top
            if (category == "SILVER") {
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.18f),
                            0.25f to Color.Transparent
                        )
                    )
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {

                // ── Heading ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Icon circle
                    val iconRes = getGoalIconResInternal(category)
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (category == "SAVINGS_PLUS") {
                                Image(
                                    painter = painterResource(Res.drawable.savings_plus),
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (iconRes != null) {
                                Image(
                                    painter = painterResource(iconRes),
                                    contentDescription = goal.name,
                                    modifier = Modifier.size(30.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(goal.iconType.ifBlank { "🎯" }, fontSize = 20.sp)
                            }
                        }
                    }

                    if (category == "SAVINGS_PLUS") {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    append("Savings ")
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("Plus")
                                    }
                                },
                                style = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = titleColor,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showSavingsPlusInfo = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Info",
                                    tint = titleColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            // Instant Pill
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Instant",
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = formatGoalNameInternal(goal.name),
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = titleColor,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                }

                if (showSavingsPlusInfo) {
                    AlertDialog(
                        onDismissRequest = { showSavingsPlusInfo = false },
                        title = {
                            Text(
                                text = "Savings Plus",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "Pyllar Savings Plus allows you to earn higher returns than a typical savings account with instant liquidity.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showSavingsPlusInfo = false }) {
                                Text("OK")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Content Row (Divided) ──────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // LEFT: Daily + Amt
                    Column(
                        modifier = Modifier.weight(0.28f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Daily",
                            style = TextStyle(fontSize = 11.sp, color = LabelText, fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = when (category) {
                                "GOLD", "SAVINGS" -> "₹21 - ₹500"
                                else              -> "₹101 - ₹500"
                            },
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(Color.Gray.copy(alpha = 0.15f))
                    )

                    // RIGHT: Benefit text + Chevron
                    Row(
                        modifier = Modifier.weight(0.65f).padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val date = bucketData?.startDate ?: "Jan 2023"
                        val infoText = buildAnnotatedString {
                            val hl = SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)
                            when (category) {
                                "GOLD" -> {
                                    val w = bucketData?.accumulatedUnits?.let { "~${it.format(1)}g" } ?: "~15.8g"
                                    append("Investing ₹101 daily since $date gives you purchase power of ")
                                    withStyle(hl) { append(w) }
                                    append(" Gold.")
                                }
                                "SILVER" -> {
                                    val w = bucketData?.accumulatedUnits?.let { "~${it.format(2)}kg" } ?: "~1.24kg"
                                    append("Investing ₹101 daily since $date yields ")
                                    withStyle(hl) { append(w) }
                                    append(" Silver worth.")
                                }
                                "SAVINGS", "SAVINGS_PLUS" -> {
                                    val c = bucketData?.currentValuation?.let {
                                        "~₹${(it / 100000.0).format(2)} Lakhs"
                                    } ?: "~₹1.24 Lakhs"
                                    append("Investments since $date build a corpus of ")
                                    withStyle(hl) { append(c) }
                                    append(".")
                                }
                                else -> append(getCorrelationTextInternal(category))
                            }
                        }

                        Text(
                            text = infoText,
                            style = TextStyle(fontSize = 12.sp, color = SecondaryText, lineHeight = 16.sp),
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )

                        // Arrow chevron button
                        ChevronButton(onClick = onClick)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Growth tag pill ───────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.48f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = titleColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = getCorrelationTextInternal(category),
                            style = TextStyle(fontSize = 11.sp, color = titleColor, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

private fun Double.format(digits: Int): String {
    val factor = when(digits) {
        1 -> 10.0
        2 -> 100.0
        else -> 1.0
    }
    return if (digits == 0) {
        (this + 0.5).toLong().toString()
    } else {
        ((this * factor).toLong() / factor).toString()
    }
}

@Composable
private fun getGoalIconResInternal(category: String): org.jetbrains.compose.resources.DrawableResource? = when (category.uppercase()) {
    "GOLD"           -> Res.drawable.gold_icon
    "SILVER"         -> Res.drawable.silver_icon
    "FESTIVAL_SPENDS"-> Res.drawable.festivals_icon
    "SAVINGS", "SAVINGS_PLUS" -> Res.drawable.savings_icon
    else             -> null
}

private fun getCorrelationTextInternal(category: String) = when (category.uppercase()) {
    "GOLD"    -> "Grows in line with gold price"
    "SILVER"  -> "Grows in line with silver price"
    "SAVINGS", "SAVINGS_PLUS" -> "Expected growth up to 7%"
    else      -> "Grows with market performance"
}

private fun formatGoalNameInternal(name: String) = when {
    name.contains("Children's Education", ignoreCase = true) -> "Education"
    name.contains("Festival Spends", ignoreCase = true)      -> "Festivals"
    else -> name
}
