package com.pyllar.consumer.presentation.support

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.presentation.ui.theme.cardBackground
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.util.AppConstants
import com.pyllar.consumer.presentation.ui.components.rememberDebouncedClick
import com.pyllar.consumer.util.BackHandler
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.*

// ── V2 visual language: cream surface, dark bronze ink, luxury gold accents ──
private val V2Cream = Color(0xFFFBF9F4)
private val V2CreamTint = Color(0xFFF5EEDB)
private val V2BronzeInk = Color(0xFF3E2723)
private val V2BronzeMuted = Color(0xFF6D4C41)
private val V2GoldDeep = Color(0xFF8B6B25)
private val V2GoldAccent = Color(0xFFD4AF37)
private val V2Obsidian = Color(0xFF0A2415)
private val V2LinkGreen = Color(0xFF1A7A42)
private val V2VolatilityRed = Color(0xFFC62828)
private val V2SuccessGreen = Color(0xFF2E7D32)
private val V2FieldBorder = Color(0xFFD7CCC8)
private val V2CardBorder = Color(0xFFEFEBE9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreenV2(
    onClose: () -> Unit = {},
    showKycHelp: Boolean = false,
    showBankHelp: Boolean = false,
    showOnlyKycInfo: Boolean = false,
    platformActions: PlatformActions = koinInject()
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // State for bottom sheet
    var selectedTopic by remember { mutableStateOf<String?>(null) }

    // Log screen view
    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("HelpSupportV2")
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // Key for the support categories section (header section)
    val supportCategoriesKey = "support_categories_header"

    // Calculate the index of the support categories header
    val supportCategoriesIndex = remember(showKycHelp, showBankHelp) {
        var index = 0
        if (showKycHelp) index += 2 // card + spacer
        if (showBankHelp) index += 2 // card + spacer
        index // header section is at this index
    }

    val v2ColorScheme = MaterialTheme.colorScheme.copy(
        background = V2Cream,
        surface = V2Cream,
        surfaceVariant = Color.White,
        inverseSurface = Color.White, // for cardBackground
        onSurface = V2BronzeInk,
        onSurfaceVariant = V2BronzeMuted,
        primary = Color(0xFF26533E), // Dark forest green
        primaryContainer = Color.White,
        onPrimaryContainer = V2BronzeInk
    )

    MaterialTheme(colorScheme = v2ColorScheme) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = V2Cream,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.help_support),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = V2BronzeInk
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = rememberDebouncedClick {
                            PlatformAnalyticsLogger.logEvent(
                                "help_support_close_clicked_v2",
                                mapOf(
                                    "screen" to "help_support_v2",
                                    "show_kyc_help" to showKycHelp,
                                    "show_bank_help" to showBankHelp,
                                    "show_only_kyc_info" to showOnlyKycInfo
                                )
                            )
                            onClose()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = V2BronzeInk
                            )
                        }
                    },
                    actions = {
                        if ((showKycHelp || showBankHelp) && !showOnlyKycInfo) {
                            TextButton(onClick = rememberDebouncedClick {
                                PlatformAnalyticsLogger.logEvent(
                                    "help_support_chat_with_us_clicked_v2",
                                    mapOf(
                                        "screen" to "help_support_v2",
                                        "show_kyc_help" to showKycHelp,
                                        "show_bank_help" to showBankHelp
                                    )
                                )
                                scope.launch {
                                    listState.animateScrollToItem(supportCategoriesIndex)
                                }
                            }) {
                                Text(
                                    text = stringResource(Res.string.chat_with_us),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF26533E)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = V2Cream
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(V2Cream)
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // KYC Help Information Section (only shown when opened from PreVerificationScreen)
                if (showKycHelp) {
                    item {
                        KycHelpInformationCardV2()
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Bank Account Help Information Section (only shown when opened from BankDetailsScreen)
                if (showBankHelp) {
                    item {
                        BankHelpInformationCardV2()
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Only show support categories and chat options if not in read-only mode
                if (!showOnlyKycInfo) {
                    item(key = supportCategoriesKey) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.choose_query_chat),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        SupportCategoryCardV2(
                            title = stringResource(Res.string.kyc_verification),
                            description = stringResource(Res.string.help_support_kyc_description),
                            onClick = {
                                PlatformAnalyticsLogger.logEvent(
                                    "help_support_category_clicked_v2",
                                    mapOf(
                                        "category" to "kyc_verification",
                                        "screen" to "help_support_v2"
                                    )
                                )
                                selectedTopic = "KYC Verification"
                            }
                        )
                    }

                    item {
                        SupportCategoryCardV2(
                            title = stringResource(Res.string.bank_account),
                            description = stringResource(Res.string.help_support_bank_description),
                            onClick = {
                                PlatformAnalyticsLogger.logEvent(
                                    "help_support_category_clicked_v2",
                                    mapOf(
                                        "category" to "bank_account",
                                        "screen" to "help_support_v2"
                                    )
                                )
                                selectedTopic = "Bank Account"
                            }
                        )
                    }

                    item {
                        SupportCategoryCardV2(
                            title = stringResource(Res.string.sip_transactions),
                            description = stringResource(Res.string.help_support_sip_description),
                            onClick = {
                                PlatformAnalyticsLogger.logEvent(
                                    "help_support_category_clicked_v2",
                                    mapOf(
                                        "category" to "sip_transactions",
                                        "screen" to "help_support_v2"
                                    )
                                )
                                selectedTopic = "SIP Transactions"
                            }
                        )
                    }

                    item {
                        SupportCategoryCardV2(
                            title = stringResource(Res.string.redemption),
                            description = stringResource(Res.string.help_support_redemption_description),
                            onClick = {
                                PlatformAnalyticsLogger.logEvent(
                                    "help_support_category_clicked_v2",
                                    mapOf(
                                        "category" to "redemption",
                                        "screen" to "help_support_v2"
                                    )
                                )
                                selectedTopic = "Redemption"
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // WhatsApp Support Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.whatsapp_support),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(Res.string.whatsapp_support_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Message input
    if (selectedTopic != null) {
        HelpIssueBottomSheet(
            topic = selectedTopic!!,
            onDismiss = {
                selectedTopic = null
            },
            onSendMessage = { userMessage ->
                val formattedMessage = "$selectedTopic\n\n$userMessage"
                PlatformAnalyticsLogger.logEvent(
                    "help_support_send_message_clicked",
                    mapOf(
                        "topic" to selectedTopic!!,
                        "screen" to "help_support_v2"
                    )
                )
                platformActions.openWhatsApp(AppConstants.SUPPORT_WHATSAPP_NUMBER, formattedMessage)
                selectedTopic = null
            }
        )
    }
}

@Composable
private fun SupportCategoryCardV2(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.tap_chat),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Message,
                    contentDescription = "WhatsApp",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpIssueBottomSheet(
    topic: String,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    var userMessage by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = { /* Do nothing to prevent dismiss on click outside/scrim */ },
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        BackHandler {
            // Do nothing to block system back press dismissal
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Close button (X) inside a circle button at top right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(V2BronzeInk.copy(alpha = 0.08f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = V2BronzeInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // What can we help you with? card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = V2Cream),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, V2FieldBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "What can we help you with?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = V2BronzeInk
                        )
                    )

                    Text(
                        text = "[$topic]",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF26533E)
                        )
                    )

                    // Text Box container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, V2FieldBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (userMessage.isEmpty()) {
                            Text(
                                text = "Describe your issue...",
                                color = V2BronzeMuted,
                                fontSize = 16.sp
                            )
                        }

                        BasicTextField(
                            value = userMessage,
                            onValueChange = { newVal ->
                                if (newVal.length <= 1000) {
                                    userMessage = newVal
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 20.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = V2BronzeInk,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFF26533E))
                        )

                        // Character counter
                        Text(
                            text = "${userMessage.length}/1000",
                            style = MaterialTheme.typography.bodySmall.copy(color = V2BronzeMuted),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        )
                    }

                }
            }

            // Send Message Button (only active if user entered some message text)
            val isButtonEnabled = userMessage.trim().isNotEmpty()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(V2GoldAccent, V2GoldDeep)), RoundedCornerShape(12.dp))
                    .padding(1.5.dp)
            ) {
                Button(
                    onClick = rememberDebouncedClick {
                        if (isButtonEnabled) {
                            onSendMessage(userMessage)
                        }
                    },
                    enabled = isButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = V2Obsidian,
                        contentColor = V2Cream,
                        disabledContainerColor = V2Obsidian,
                        disabledContentColor = V2Cream
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Send Message",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Spacing below the send message button
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun KycHelpInformationCardV2() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, V2FieldBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.kyc_help_information),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.why_complete_kyc),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.kyc_mandatory_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
            }

            HorizontalDivider(color = V2FieldBorder)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.what_information_needed),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.kyc_ask_for),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.pan_card_details),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    Text(
                        text = stringResource(Res.string.basic_personal_financial),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }

            HorizontalDivider(color = V2FieldBorder)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.step_by_step_process),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Step 1: PAN Verification",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.step_1_pan_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = stringResource(Res.string.mismatch_may_fail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Start
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.step_2_personal),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.step_2_we_need),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start
                    )
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.address_information),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Text(
                            text = stringResource(Res.string.income_occupation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Text(
                            text = stringResource(Res.string.nominee_details_bullet),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.step_3_bank),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.step_3_bank_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = stringResource(Res.string.account_name_upi),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.step_4_digilocker),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.step_4_required_if),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = stringResource(Res.string.aadhaar_secure_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
            }

            HorizontalDivider(color = V2FieldBorder)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.how_long_kyc),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.fill_form_minutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }

            HorizontalDivider(color = V2FieldBorder)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.need_help_kyc),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.kyc_tap_whatsapp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun BankHelpInformationCardV2() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, V2FieldBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.bank_account_help_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.why_link_bank),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.we_need_bank_to),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.process_sip_securely),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    Text(
                        text = stringResource(Res.string.credit_withdrawals),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    Text(
                        text = stringResource(Res.string.verify_identity_regulations),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }

            HorizontalDivider(color = V2FieldBorder)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.account_requirements),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.bank_account_must_be),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.individual_savings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    Text(
                        text = stringResource(Res.string.not_joint_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    Text(
                        text = stringResource(Res.string.active_upi_linked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }

            HorizontalDivider(color = V2FieldBorder)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.setup_process),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.step_1_enter_bank),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.provide_bank),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start
                    )
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.bank_account_number_bullet),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Text(
                            text = stringResource(Res.string.ifsc_code_bullet),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                    Text(
                        text = stringResource(Res.string.details_accurate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.step_2_choose_sip),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.select_invest_regularly),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.step_3_account_verify),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.once_kyc_approved),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start
                    )
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.we_ask_verify_bank),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Text(
                            text = stringResource(Res.string.refundable_1_payment),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Text(
                            text = stringResource(Res.string.refund_2_3_days),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.step_4_start_sip),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.after_verification_sip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start
                    )
                }
            }

            HorizontalDivider(color = V2FieldBorder)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.common_question),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.what_if_verification_fails),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(Res.string.double_check_account_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            textAlign = TextAlign.Start
                        )
                    }

                    HorizontalDivider(color = V2FieldBorder)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.need_help_kyc),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(Res.string.bank_need_help),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}
