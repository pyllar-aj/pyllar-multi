package com.pyllar.consumer.presentation.loan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

private val Cream = Color(0xFFFBF9F4)
private val Obsidian = Color(0xFF0A2415)
private val DeepGreen = Color(0xFF11381E)
private val Gold = Color(0xFFD4AF37)
private val DarkGold = Color(0xFF8B6B25)
private val Brown = Color(0xFF3E2723)
private val BrownMuted = Color(0xFF6D4C41)
private val Hairline = Color(0xFFEFEBE9)
private val Link = Color(0xFF1A7A42)

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun displayDate(iso: String?): String =
    runCatching {
        if (iso.isNullOrBlank()) return@runCatching "—"
        val d = LocalDate.parse(iso.take(10))
        val monthName = MONTH_NAMES.getOrNull(d.monthNumber - 1) ?: d.monthNumber.toString()
        "${d.dayOfMonth} $monthName ${d.year}"
    }.getOrDefault("—")

private fun statusLabel(status: String?) = when (status) {
    "NO_OFFERS" -> "No offers available"
    "REJECTED" -> "Not approved"
    else -> "Closed"
}

/** Shown when the loans card is tapped while a recent NO_OFFERS/REJECTED blocks a new application. */
@Composable
fun LoanCooldownScreen(
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    viewModel: LoanCooldownViewModel = koinInject()
) {
    val ui by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
            Text(
                "Help", modifier = Modifier.clickable(onClick = onHelpClick),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Link
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Loan application", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen
            )
            Text(
                "YOUR LAST APPLICATION", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp, color = DarkGold
            )

            val entry = ui.entry
            when {
                ui.isLoading -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Gold, strokeWidth = 3.dp)
                }
                entry == null || !entry.cooldownActive -> {
                    // Nothing to wait for (or the call failed): let the user retry or go back.
                    Text(
                        if (ui.hasError) "We could not load this right now." else "You can apply for a loan again.",
                        fontSize = 16.sp, color = BrownMuted
                    )
                    Button(
                        onClick = if (ui.hasError) viewModel::load else onBackClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Obsidian, contentColor = Color.White)
                    ) { Text(if (ui.hasError) "Try again" else "Back", fontWeight = FontWeight.Bold) }
                }
                else -> {
                    Text(
                        "Come back in ${entry.daysRemaining} ${if (entry.daysRemaining == 1) "day" else "days"}",
                        fontSize = 32.sp, lineHeight = 35.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailRow("Applied on", displayDate(entry.appliedOn))
                        DetailRow("Outcome", statusLabel(entry.status))
                        DetailRow("You can apply again on", displayDate(entry.retryOn))
                    }
                    Text(
                        "Lenders could not make an offer this time. To protect your credit score, you can " +
                            "apply again after 30 days. This application does not affect your score.",
                        fontSize = 14.sp, lineHeight = 21.sp, color = BrownMuted, textAlign = TextAlign.Start
                    )
                }
            }
        }

        Button(
            onClick = onBackClick,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, Gold),
            colors = ButtonDefaults.buttonColors(containerColor = Obsidian, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(52.dp)
        ) { Text("Back to home", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 13.sp, color = BrownMuted)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DeepGreen)
    }
}

