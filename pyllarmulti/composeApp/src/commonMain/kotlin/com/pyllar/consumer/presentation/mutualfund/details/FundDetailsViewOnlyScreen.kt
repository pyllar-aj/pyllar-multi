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

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 32.dp),
                title = { Text(state.fundDetails?.fundName ?: "Fund Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
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
                        
                        FundMetricsGrid(
                            expenseRatio = details.expenseRatio?.toString() ?: "-",
                            aum = details.aum?.toString() ?: "-",
                            exitLoad = details.exitLoad?.toString() ?: "-"
                        )
                        
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
                                color = Color.Black
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
                                    color = MaterialTheme.colorScheme.onSurface
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
                                    color = MaterialTheme.colorScheme.primary,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
