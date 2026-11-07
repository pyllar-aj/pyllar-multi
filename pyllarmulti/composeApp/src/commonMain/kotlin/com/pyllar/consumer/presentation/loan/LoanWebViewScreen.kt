package com.pyllar.consumer.presentation.loan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.util.BackHandler

private val Cream = Color(0xFFFBF9F4)
private val DeepGreen = Color(0xFF11381E)
private val Brown = Color(0xFF3E2723)
private val Hairline = Color(0xFFEFEBE9)

/**
 * The lender's hosted journey for the offer the user picked.
 * Closing returns to the offers screen, which refreshes the application's status when it resumes.
 */
@Composable
fun LoanWebViewScreen(
    url: String,
    onClose: () -> Unit
) {
    BackHandler {
        onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onClose, shape = CircleShape, color = Color.White,
                border = BorderStroke(1.dp, Hairline), modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Brown)
                }
            }
            Text(
                "Complete your loan", modifier = Modifier.padding(start = 12.dp),
                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LoanPlatformWebView(
                url = url,
                modifier = Modifier.fillMaxSize(),
                onClose = onClose
            )
        }
    }
}

