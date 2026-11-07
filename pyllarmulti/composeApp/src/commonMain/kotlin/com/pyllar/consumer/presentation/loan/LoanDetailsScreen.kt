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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** The loan the user took: lender, amounts, term and the lender's documents, from what we stored at acceptance. */
@Composable
fun LoanDetailsScreen(
    applicationId: String,
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    viewModel: LoanDetailsViewModel = koinInject()
) {
    LaunchedEffect(applicationId) {
        viewModel.initApplicationId(applicationId)
    }

    val ui by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Your loan", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen)

            val info = ui.info
            when {
                ui.isLoading -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Gold, strokeWidth = 3.dp)
                }
                info == null -> {
                    Text("We could not load your loan right now.", fontSize = 16.sp, color = BrownMuted)
                    Button(
                        onClick = { viewModel.load(applicationId) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Obsidian, contentColor = Color.White)
                    ) { Text("Try again", fontWeight = FontWeight.Bold) }
                }
                else -> {
                    Text(
                        info.lenderName.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp, color = DarkGold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(info.amount, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen)
                        Text(
                            "DISBURSED", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                            color = Color(0xFFFFECB3),
                            modifier = Modifier
                                .background(Obsidian, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        info.rows.forEach { (label, value) -> DetailRow(label, value) }
                    }

                    if (info.kfsUrl != null || info.tncUrl != null) {
                        Text(
                            "DOCUMENTS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp, color = DarkGold
                        )
                        info.kfsUrl?.let { DocLink("Key facts statement") { uriHandler.openUri(it) } }
                        info.tncUrl?.let { DocLink("Terms and conditions") { uriHandler.openUri(it) } }
                    }

                    Text(
                        "${info.lenderName} manages your repayments. For payment or statement questions, " +
                            "contact them directly, or reach us through Help.",
                        fontSize = 14.sp, lineHeight = 21.sp, color = BrownMuted
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

@Composable
private fun DocLink(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brown)
        Text("Open ›", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Link)
    }
}

