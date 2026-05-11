package com.pyllar.consumer.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import pyllar.composeapp.generated.resources.*

@Composable
fun TrustStrip(
    onInfoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heading with info icon - center aligned
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Powered by leading AMCs",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(3.dp))
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Horizontal scrollable logos - center aligned
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Axis
                AmcLogo(
                    logoRes = Res.drawable.axis_lo,
                    contentDescription = "Axis Mutual Fund"
                )
                // Invesco
                AmcLogo(
                    logoRes = Res.drawable.invesco,
                    contentDescription = "Invesco"
                )
                // Aditya Birla
                AmcLogo(
                    logoRes = Res.drawable.aditya,
                    contentDescription = "Aditya Birla"
                )
                // Nippon
                AmcLogo(
                    logoRes = Res.drawable.nippon,
                    contentDescription = "Nippon"
                )
            }
        }
    }
}

@Composable
fun AmcLogo(
    logoRes: org.jetbrains.compose.resources.DrawableResource,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(logoRes),
            contentDescription = contentDescription,
            modifier = Modifier.height(20.dp),
            contentScale = ContentScale.Fit
        )
    }
}
