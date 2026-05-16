package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.painterResource
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.*
import io.github.alexzhirkevich.compottie.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.grid.items

@Composable
fun AmountChip(
    amount: Int? = null,
    label: String? = null,
    isSelected: Boolean,
    isPopular: Boolean,
    onClick: () -> Unit
) {
    val text = label ?: "₹$amount"
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Box(modifier = Modifier.width(72.dp).height(70.dp).clickable(onClick = onClick)) {
        Card(
            modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        if (isPopular) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .zIndex(2f)
            ) {
                Text("POPULAR", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusDisplay(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(80.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(actionText, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LoadingDisplay() {
    val composition by rememberLottieComposition {
        val json = Res.readBytes("files/secure.json").decodeToString()
        LottieCompositionSpec.JsonString(json)
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            "Verifying...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Please do not close the app or go back.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun getFallbackUpiIcon(displayName: String): Painter? {
    val name = displayName.lowercase()
    return when {
        name.contains("phonepe") -> painterResource(Res.drawable.upi_phonepe)
        name.contains("google pay") || name.contains("gpay") || name.contains("tez") -> painterResource(Res.drawable.upi_gpay)
        name.contains("paytm") -> painterResource(Res.drawable.upi_paytm)
        name.contains("bhim") -> painterResource(Res.drawable.upi_bhim)
        name.contains("amazon") -> painterResource(Res.drawable.upi_amazonpay)
        name.contains("cred") -> painterResource(Res.drawable.upi_cred)
        name.contains("imobile") || name.contains("icici") -> painterResource(Res.drawable.upi_imobile)
        else -> null
    }
}

@Composable
fun UpiAppGrid(
    apps: List<com.pyllar.consumer.platform.UpiAppInfo>,
    onAppClick: (com.pyllar.consumer.platform.UpiAppInfo) -> Unit,
    onMoreClick: (() -> Unit)? = null,
    maxRows: Int = 2
) {
    val hasMoreApps = onMoreClick != null && apps.size > 6
    val appsToShow = if (hasMoreApps) apps.take(5) else apps.take(6)

    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().heightIn(max = (80 * maxRows).dp)
    ) {
        items(appsToShow) { app ->
            UpiAppCard(app = app, onClick = { onAppClick(app) })
        }
        if (hasMoreApps && onMoreClick != null) {
            item {
                Card(
                    onClick = onMoreClick,
                    modifier = Modifier.height(80.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("More...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun UpiAppCard(app: com.pyllar.consumer.platform.UpiAppInfo, onClick: () -> Unit) {
    val fallbackIcon = getFallbackUpiIcon(app.displayName)
    
    Card(
        onClick = onClick,
        modifier = Modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (app.icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = app.icon!!, 
                    contentDescription = app.displayName, 
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else if (fallbackIcon != null) {
                androidx.compose.foundation.Image(
                    painter = fallbackIcon,
                    contentDescription = app.displayName,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary), 
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.displayName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(app.displayName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun QrPlaceholder(url: String, description: String = "Scan with any UPI app") {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (url.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = io.github.alexzhirkevich.qrose.rememberQrCodePainter(url),
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun InvestingInCard(
    fundDetailsState: com.pyllar.consumer.presentation.mutualfund.details.FundDetailsState,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Investing in", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                fundDetailsState.fundDetails?.fundName?.let { fundName ->
                    val logo = when {
                        fundName.contains("Invesco", true) -> Res.drawable.invesco
                        fundName.contains("Aditya", true) -> Res.drawable.aditya
                        fundName.contains("Axis", true) -> Res.drawable.axis_lo
                        fundName.contains("Nippon", true) -> Res.drawable.nippon
                        else -> null
                    }
                    if (logo != null) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(logo),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            fundDetailsState.fundDetails?.let { details ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = details.fundName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(details.category ?: "Mutual Fund", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Risk Level", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(details.riskLevel ?: "Moderate", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            } ?: run {
                if (fundDetailsState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    Text("Fund details not available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
