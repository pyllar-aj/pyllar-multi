@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pyllar.consumer.presentation.loan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.data.remote.requests.LoanOfferDto
import com.pyllar.consumer.presentation.dashboard.LoanCardInfo
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private val Cream = Color(0xFFFBF9F4)
private val Obsidian = Color(0xFF0A2415)
private val DeepGreen = Color(0xFF11381E)
private val Gold = Color(0xFFD4AF37)
private val DarkGold = Color(0xFF8B6B25)
private val Brown = Color(0xFF3E2723)
private val BrownMuted = Color(0xFF6D4C41)
private val Hairline = Color(0xFFEFEBE9)
private val Success = Color(0xFF2E7D32)
private val Link = Color(0xFF1A7A42)
private val TagBg = Color(0xFFF5EBCB)
private val TagFg = Color(0xFF5F4818)

private fun inr(v: Double?) = if (v == null) "—" else LoanCardInfo.rupees(v)

/** ₹1L / ₹40K style used for the lender range. */
private fun short(v: Double?): String? = when {
    v == null -> null
    v >= 100_000 -> "₹" + trim(v / 100_000) + "L"
    else -> "₹" + trim(v / 1_000) + "K"
}

private fun trim(d: Double): String {
    val rounded = kotlin.math.round(d * 10.0) / 10.0
    val integerPart = rounded.toLong()
    val fracPart = kotlin.math.round((rounded - integerPart) * 10.0).toLong()
    return if (fracPart == 0L) integerPart.toString() else "$integerPart.$fracPart"
}

private fun shortName(name: String) = if (name.startsWith("Cholamandalam")) "Cholamandalam Finance" else name

@Composable
fun LoanOffersScreen(
    applicationId: String,
    onBackClick: () -> Unit,
    onOpenHosted: (String) -> Unit = {},
    viewModel: LoanOffersViewModel = koinInject()
) {
    val ui by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(applicationId) {
        viewModel.initApplicationId(applicationId)
    }

    DisposableEffect(applicationId) {
        viewModel.startPolling()
        onDispose {
            viewModel.stopPolling()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoanOffersEvent.OpenHosted -> onOpenHosted(event.url)
                LoanOffersEvent.AssistedCallback -> {
                    toastMessage = "A Pyllar partner will call you to complete this loan."
                }
            }
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3500)
            toastMessage = null
        }
    }

    val selected = ui.offers.firstOrNull { it.offerId == ui.selectedOfferId }
    val lowestApr = ui.offers.mapNotNull { it.apr }.minOrNull().takeIf { ui.offers.size > 1 }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onBackClick, shape = CircleShape, color = Color.White,
                                border = BorderStroke(1.dp, Hairline), modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Brown)
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                            Text("Loan application", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen)
                            Text(
                                "STEP 2 OF 2 · CHOOSE AN OFFER", modifier = Modifier.padding(top = 4.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, color = DarkGold
                            )
                            Text(
                                "Your loan offers", modifier = Modifier.padding(top = 8.dp),
                                fontSize = 32.sp, lineHeight = 35.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen
                            )
                            headline(ui)?.let {
                                Text(it, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold)
                            }
                        }
                    }
                }

                if (ui.offers.isEmpty()) {
                    item {
                        EmptyOrWaiting(
                            ui = ui,
                            onOpenAa = { url -> runCatching { uriHandler.openUri(url) } }
                        )
                    }
                } else {
                    items(ui.offers, key = { it.offerId }) { offer ->
                        OfferCard(
                            offer = offer,
                            selected = offer.offerId == ui.selectedOfferId,
                            lowestApr = offer.apr != null && offer.apr == lowestApr,
                            onSelect = { viewModel.select(offer.offerId) },
                            onOpenLink = { url -> runCatching { uriHandler.openUri(url) } },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                    if (ui.pendingLenders > 0) {
                        item { MoreComing(ui.pendingLenders) }
                    }
                    item {
                        Text(
                            "✓ Short-term loans show a high APR because it is calculated per year, even though you repay in days · " +
                                "Compare “You repay” for the real cost",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            fontSize = 13.sp, lineHeight = 19.sp, color = BrownMuted
                        )
                    }
                    ui.disclaimer?.let { notice ->
                        item {
                            Text(
                                notice, modifier = Modifier.padding(horizontal = 20.dp),
                                fontSize = 13.sp, lineHeight = 19.sp, color = BrownMuted
                            )
                        }
                    }
                }

                if (ui.connectionProblem) {
                    item {
                        Text(
                            "Having trouble reaching Pyllar. Retrying…",
                            modifier = Modifier.padding(horizontal = 20.dp), fontSize = 13.sp, color = BrownMuted
                        )
                    }
                }
            }

            if (ui.offers.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Cream)
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
                ) {
                    ui.errorMessage?.let {
                        Text(it, modifier = Modifier.padding(bottom = 8.dp), fontSize = 14.sp, color = Color(0xFFB00020))
                    }
                    Button(
                        onClick = viewModel::continueWithSelected,
                        enabled = selected != null && !ui.isContinuing,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Gold),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Obsidian, contentColor = Color.White,
                            disabledContainerColor = Obsidian.copy(alpha = 0.6f), disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (ui.isContinuing) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                selected?.let { "Continue with ${shortName(it.lenderName)}" } ?: "Select an offer",
                                fontSize = 17.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // In-app Toast message
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Obsidian,
                border = BorderStroke(1.dp, Gold),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = toastMessage.orEmpty(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

private fun headline(ui: LoanOffersUiState): String? = when {
    ui.offers.isNotEmpty() && ui.pendingLenders > 0 ->
        "${ui.offers.size} ${if (ui.offers.size == 1) "offer" else "offers"} so far"
    ui.offers.isNotEmpty() -> "${ui.offers.size} ${if (ui.offers.size == 1) "lender made" else "lenders made"} an offer"
    ui.isFirstLoad || ui.status == "OFFERS_PENDING" || ui.status == "STARTING" -> "Checking with lenders"
    else -> null
}

@Composable
private fun MoreComing(pending: Int) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Gold, strokeWidth = 2.dp)
        Text(
            "Waiting for $pending more ${if (pending == 1) "lender" else "lenders"}. New offers will appear here.",
            fontSize = 14.sp, color = BrownMuted
        )
    }
}

@Composable
private fun EmptyOrWaiting(ui: LoanOffersUiState, onOpenAa: (String) -> Unit) {
    val (title, body) = when {
        ui.isFirstLoad || ui.status == "OFFERS_PENDING" || ui.status == "STARTING" ->
            "Looking for offers" to "This usually takes under a minute. You can stay on this screen — offers show up as lenders reply."
        ui.status == "AA_REQUIRED" ->
            "One more step" to "Share your bank statement securely so lenders can see your income and make an offer."
        ui.status == "HANDED_OFF" ->
            "Application in progress" to "You have already picked an offer. Finish the steps with the lender to get your money."
        ui.status == "NOT_PROFILED" || ui.status == "REJECTED" ->
            "No offers right now" to "We could not match you with a lender this time. This does not affect your credit score."
        ui.status == "EXPIRED" -> "This application expired" to "Please start a new loan application."
        ui.status == "FAILED" -> "Something went wrong" to "Please start a new loan application."
        else -> "No offers right now" to "No lender could make an offer for you at this time. This does not affect your credit score."
    }
    val waiting = ui.isFirstLoad || ui.status == "OFFERS_PENDING" || ui.status == "STARTING"
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (waiting) CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Gold, strokeWidth = 3.dp)
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen, textAlign = TextAlign.Center)
        Text(body, fontSize = 14.sp, lineHeight = 21.sp, color = BrownMuted, textAlign = TextAlign.Center)
        if (ui.status == "AA_REQUIRED" && ui.accountAggregatorUrl != null) {
            Button(
                onClick = { onOpenAa(ui.accountAggregatorUrl) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Gold),
                colors = ButtonDefaults.buttonColors(containerColor = Obsidian, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Share bank statement", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfferCard(
    offer: LoanOfferDto,
    selected: Boolean,
    lowestApr: Boolean,
    onSelect: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val multi = (offer.installments ?: 1) > 1
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(if (selected) Color(0xFFFFFBEF) else Color.White, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) Gold else Hairline, shape)
            .clickable(onClick = onSelect)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(shortName(offer.lenderName), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen)
                val lo = short(offer.lenderMinAmount)
                val hi = short(offer.lenderMaxAmount)
                if (lo != null && hi != null) Text("Lends $lo – $hi", fontSize = 13.sp, color = BrownMuted)
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(if (selected) Obsidian else Color.White, CircleShape)
                    .border(2.dp, if (selected) Obsidian else Color(0xFFBDBDBD), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (lowestApr) Tag("LOWEST APR", Obsidian, Color(0xFFFFECB3))
            offer.term?.takeIf { it.isNotBlank() }?.let { Tag(it.uppercase(), TagBg, TagFg) }
            if (!multi) Tag("SINGLE PAYMENT", TagBg, TagFg)
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Loan amount", fontSize = 13.sp, color = BrownMuted)
                Text(inr(offer.principal), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Brown)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (multi) "EMI · ${offer.installments} months" else "Repay in ${offer.term.orEmpty()}",
                    fontSize = 13.sp, color = BrownMuted
                )
                Text(
                    inr(if (multi) offer.installment else offer.totalPayable ?: offer.installment),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Success
                )
            }
        }

        val stats = listOf(
            "You get in bank" to inr(offer.netDisbursed),
            "You repay" to inr(offer.totalPayable),
            "Interest rate" to ((offer.interestRate?.let { trim(it) + "%" } ?: "—") + if (multi) " p.a." else " per day"),
            "APR" to (offer.apr?.let { trim(it) + "%" } ?: "—"),
            "Processing fee" to inr(offer.processingFee?.toDoubleOrNull()),
            "Total interest" to inr(offer.totalInterest)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                .background(Hairline, RoundedCornerShape(12.dp)),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            stats.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    pair.forEach { (k, v) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFFFFDF8))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(k, fontSize = 12.sp, color = BrownMuted)
                            Text(v, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Brown)
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            offer.kfsUrl?.let {
                Text("Key facts statement", modifier = Modifier.clickable { onOpenLink(it) }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Link)
            }
            offer.tncUrl?.let {
                Text("Terms & conditions", modifier = Modifier.clickable { onOpenLink(it) }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Link)
            }
        }
    }
}

@Composable
private fun Tag(label: String, bg: Color, fg: Color) {
    Text(
        label,
        modifier = Modifier
            .background(bg, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = fg
    )
}

