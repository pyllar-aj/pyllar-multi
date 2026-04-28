package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeDetailsScreen(
    userId: String = "",
    purpose: String = "",
    onNavigateBack: () -> Unit = {},
    onNavigateToWithdraw: (WithdrawInitParams) -> Unit = {},
    onNavigateToAddFunds: (String) -> Unit = {},
    viewModel: SchemeDetailsViewModel = koinInject()
) {
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    val cancelSipResult by viewModel.cancelSipResult.collectAsState()
    val pauseSipResult by viewModel.pauseSipResult.collectAsState()
    val resumeSipResult by viewModel.resumeSipResult.collectAsState()
    
    val cancelSipLoading by viewModel.cancelSipLoading.collectAsState()
    val pauseSipLoading by viewModel.pauseSipLoading.collectAsState()
    val resumeSipLoading by viewModel.resumeSipLoading.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showDetailsPopup by remember { mutableStateOf(false) }
    var showPauseConfirm by remember { mutableStateOf<MandateDisplayItem?>(null) }
    var showResumeConfirm by remember { mutableStateOf<MandateDisplayItem?>(null) }
    var showCancelConfirm by remember { mutableStateOf<MandateDisplayItem?>(null) }

    val schemeParams = remember { SchemeDetailsParamsManager.get() }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("SchemeDetails")
    }

    LaunchedEffect(userId, purpose) {
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            viewModel.loadTransactions(userId, purpose, schemeParams)
        }
    }

    // Handle SIP Action Results
    LaunchedEffect(cancelSipResult) {
        if (cancelSipResult is CancelSipResult.Success) {
            showCancelConfirm = null
            viewModel.loadTransactions(userId, purpose, schemeParams)
            viewModel.clearCancelSipResult()
        }
    }
    LaunchedEffect(pauseSipResult) {
        if (pauseSipResult is PauseSipResult.Success) {
            showPauseConfirm = null
            viewModel.loadTransactions(userId, purpose, schemeParams)
            viewModel.clearPauseSipResult()
        }
    }
    LaunchedEffect(resumeSipResult) {
        if (resumeSipResult is ResumeSipResult.Success) {
            showResumeConfirm = null
            viewModel.loadTransactions(userId, purpose, schemeParams)
            viewModel.clearResumeSipResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (!state.goalName.isNullOrBlank()) formatGoalName(state.goalName!!) else "Scheme Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = getCorrelationColorForCategory(state.category, state.colorTheme)
                        )
                        Text(
                            text = formatSchemeName(state.schemeName ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alpha(0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.schemeName?.firstOrNull()?.toString() ?: "P",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = getCorrelationColorForCategory(state.category, state.colorTheme)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                LoadingScreen(text = "Loading details...", modifier = Modifier.fillMaxSize())
            } else if (state.errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = state.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadTransactions(userId, purpose, schemeParams) }) {
                        Text("Retry")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            SchemeDetailsCard(
                                state = state,
                                onViewDetails = { showDetailsPopup = true }
                            )
                        }

                        item {
                            val goalColor = getCorrelationColorForCategory(state.category, state.colorTheme)
                            TabRow(
                                selectedTabIndex = selectedTabIndex,
                                containerColor = Color.Transparent,
                                divider = {},
                                indicator = { tabPositions ->
                                    Box(
                                        Modifier
                                            .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                            .height(3.dp)
                                            .padding(horizontal = 16.dp)
                                            .background(goalColor, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    )
                                }
                            ) {
                                Tab(
                                    selected = selectedTabIndex == 0,
                                    onClick = { selectedTabIndex = 0 },
                                    text = { 
                                        Text(
                                            "Transactions", 
                                            color = if (selectedTabIndex == 0) goalColor else Color.Gray,
                                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    }
                                )
                                Tab(
                                    selected = selectedTabIndex == 1,
                                    onClick = { selectedTabIndex = 1 },
                                    text = { 
                                        Text(
                                            "Active Plans", 
                                            color = if (selectedTabIndex == 1) goalColor else Color.Gray,
                                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    }
                                )
                            }
                        }

                        if (selectedTabIndex == 0) {
                            if (state.transactions.isEmpty()) {
                                item {
                                    Text(
                                        "No transactions found",
                                        modifier = Modifier.fillMaxWidth().padding(32.dp).alpha(0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                items(state.transactions) { tx ->
                                    TransactionRowRefined(tx)
                                }
                            }
                        } else {
                            if (state.mandates.isEmpty()) {
                                item {
                                    Text(
                                        "No active plans found",
                                        modifier = Modifier.fillMaxWidth().padding(32.dp).alpha(0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                items(state.mandates) { mandate ->
                                    MandateRowRefined(
                                        mandate = mandate,
                                        onPause = { showPauseConfirm = mandate },
                                        onResume = { showResumeConfirm = mandate },
                                        onCancel = { showCancelConfirm = mandate },
                                        accentColor = getCorrelationColorForCategory(state.category, state.colorTheme)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToAddFunds(purpose) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = getCorrelationColorForCategory(state.category, state.colorTheme))
                        ) {
                            Text("Invest More", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val params = WithdrawInitParams(
                                    isin = state.isin ?: "",
                                    folio = state.folioNumber,
                                    amount = state.currentValue,
                                    investmentInProgress = state.investmentInProgress,
                                    schemeName = state.schemeName
                                )
                                onNavigateToWithdraw(params)
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, getCorrelationColorForCategory(state.category, state.colorTheme))
                        ) {
                            Text("Withdraw", color = getCorrelationColorForCategory(state.category, state.colorTheme), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Action Dialogs
            showPauseConfirm?.let { mandate ->
                AlertDialog(
                    onDismissRequest = { showPauseConfirm = null },
                    title = { Text("Pause SIP") },
                    text = { Text("Are you sure you want to pause this SIP of ₹${formatIndian(mandate.amount)}?") },
                    confirmButton = {
                        Button(onClick = { viewModel.pauseSip(userId, mandate.planId, mandate.mandateId) }) {
                            if (pauseSipLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("Pause")
                        }
                    },
                    dismissButton = { TextButton(onClick = { showPauseConfirm = null }) { Text("Cancel") } }
                )
            }

            showResumeConfirm?.let { mandate ->
                AlertDialog(
                    onDismissRequest = { showResumeConfirm = null },
                    title = { Text("Resume SIP") },
                    text = { Text("Do you want to resume your SIP of ₹${formatIndian(mandate.amount)}?") },
                    confirmButton = {
                        Button(onClick = { viewModel.resumeSip(userId, mandate.planId, mandate.mandateId) }) {
                            if (resumeSipLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("Resume")
                        }
                    },
                    dismissButton = { TextButton(onClick = { showResumeConfirm = null }) { Text("Cancel") } }
                )
            }

            showCancelConfirm?.let { mandate ->
                AlertDialog(
                    onDismissRequest = { showCancelConfirm = null },
                    title = { Text("Cancel SIP") },
                    text = { Text("Cancelling your SIP will stop future investments. Are you sure?") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.cancelSip(userId, mandate.planId, mandate.mandateId, "User Request") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            if (cancelSipLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("Cancel SIP", color = Color.White)
                        }
                    },
                    dismissButton = { TextButton(onClick = { showCancelConfirm = null }) { Text("Close") } }
                )
            }
        }
    }
}

@Composable
fun SchemeDetailsCard(
    state: SchemeDetailsState,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Total Value", style = MaterialTheme.typography.labelMedium, modifier = Modifier.alpha(0.6f))
                    Text("₹${formatIndian(state.cummulativeValue)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onViewDetails) {
                    Text("View Details", color = getCorrelationColorForCategory(state.category, state.colorTheme))
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = getCorrelationColorForCategory(state.category, state.colorTheme))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4-box Summary
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryBox(label = "Invested", value = "₹${formatIndian(state.investedAmount)}", modifier = Modifier.weight(1f))
                VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                SummaryBox(label = "Returns", value = "₹${formatIndian(state.totalGain)}", modifier = Modifier.weight(1f), valueColor = if (state.totalGain >= 0) Color(0xFF2E7D32) else Color.Red)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryBox(label = "Current", value = "₹${formatIndian(state.currentValue)}", modifier = Modifier.weight(1f))
                VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                SummaryBox(label = "Processing", value = "₹${formatIndian(state.investmentInProgress)}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SummaryBox(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Black) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun TransactionRowRefined(tx: TransactionDisplayItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (tx.isCredit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (tx.isCredit) Icons.Default.Add else Icons.Default.CallReceived,
                    contentDescription = null,
                    tint = if (tx.isCredit) Color(0xFF2E7D32) else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(tx.transactionType ?: "Transaction", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(tx.date ?: "", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (tx.isCredit) "+" else "-") + "₹${formatIndian(tx.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (tx.isCredit) Color(0xFF2E7D32) else Color.Black
            )
            Text(
                text = tx.state,
                style = MaterialTheme.typography.labelSmall,
                color = when(tx.state) {
                    "SUCCESS" -> Color(0xFF2E7D32)
                    "SUBMITTED" -> Color(0xFF1565C0)
                    "FAILED" -> Color.Red
                    else -> Color.Gray
                }
            )
        }
    }
}

@Composable
fun MandateRowRefined(
    mandate: MandateDisplayItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Monthly SIP", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
                    Text("₹${formatIndian(mandate.amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Next Date", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
                    Text(mandate.nextSipDate ?: "N/A", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }

            val status = mandate.status?.uppercase() ?: ""
            val isPaused = status.contains("PAUSED")
            val isActive = (status.contains("ACTIVE") || status.contains("APPROVED")) && !isPaused

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isActive) {
                    Button(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause", fontSize = 12.sp)
                    }
                } else if (isPaused) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", fontSize = 12.sp)
                    }
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel", color = Color.Red, fontSize = 12.sp)
                }
            }
        }
    }
}
