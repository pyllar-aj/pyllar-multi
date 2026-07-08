package com.pyllar.consumer.presentation.mutualfund.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.presentation.mutualfund.details.*
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import org.jetbrains.compose.resources.stringResource
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
fun FundDetailsViewOnlyScreen(
    isin: String = "",
    userId: String = "",
    goalId: String = "",
    onBackClick: () -> Unit,
    viewModel: FundDetailsViewModel
) {
    val uriHandler = LocalUriHandler.current
    val state by viewModel.uiState.collectAsState()
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isin, userId, goalId) {
        if (isin.isNotBlank() && isin != "none") {
            viewModel.loadFundDetails(isin)
        } else if (userId.isNotBlank() && goalId.isNotBlank() && userId != "none" && goalId != "none") {
            viewModel.loadFundDetailsByGoal(userId, goalId)
        }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("FundDetailsViewOnly")
    }

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text("Disclaimer") },
            text = {
                Column {
                    Text(stringResource(Res.string.disclaimer_popup_content))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.amfi_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("OK")
                }
            }
        )
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
            containerColor = V2Cream,
            topBar = {
                TopAppBar(
                    modifier = Modifier.padding(top = 10.dp),
                    title = {
                        Text(
                            text = state.fundDetails?.fundName ?: "Fund Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = V2BronzeInk,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = V2BronzeInk
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = V2Cream,
                        titleContentColor = V2BronzeInk,
                        navigationIconContentColor = V2BronzeInk
                    )
                )
            }
        ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                state.fundDetails?.let { details ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        FundHeader(details)
                        FundChartSection(
                            state = state,
                            onPeriodSelected = { period ->
                                viewModel.onPeriodSelected(period)
                            }
                        )
                        
                        FundMetricsGrid(details)
                        
                        // Riskometer section
                        val riskLevel = remember(details.riskLevel) {
                            getLumpsumRiskLevel(details.riskLevel)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.riskometer),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = V2BronzeInk
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LumpsumSemiCircleRiskometer(
                                    riskLevel = riskLevel,
                                    size = 80.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = riskLevel.label.replace("\n", " "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = V2BronzeInk
                                )
                            }
                        }

                        LumpsumCompanyAllocationSection(companyAllocation = details.companyAllocation)

                        // Footer Links
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!details.schemeDocumentUrl.isNullOrBlank()) {
                                Text(
                                    text = stringResource(Res.string.scheme_documents),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = V2LinkGreen,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable {
                                        uriHandler.openUri(details.schemeDocumentUrl)
                                    }.padding(4.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Text(
                                text = stringResource(Res.string.disclaimer),
                                style = MaterialTheme.typography.bodyMedium,
                                color = V2BronzeMuted,
                                modifier = Modifier
                                    .clickable { showDisclaimerDialog = true }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
