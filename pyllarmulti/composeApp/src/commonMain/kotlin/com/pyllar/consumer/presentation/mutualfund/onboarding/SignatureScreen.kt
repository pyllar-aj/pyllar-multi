package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.platform.PlatformImage
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SignatureScreen(
    userId: String,
    kycAttemptId: String,
    investorId: String,
    onSignatureCompleted: (nextScreen: String?, redirectUrl: String?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    viewModel: SignatureViewModel = koinInject(),
    sessionStore: com.pyllar.consumer.domain.storage.SessionStore = koinInject()
) {
    var signaturePaths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var hasSignature by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var signatureBoxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateToHelp) {
                    Text(
                        text = "Help",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Digital Signature",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Please draw your signature in the box below as it appears on your PAN card.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Signature Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .onGloballyPositioned { coordinates ->
                            signatureBoxCoordinates = coordinates
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = Path().apply {
                                        moveTo(offset.x, offset.y)
                                    }
                                    hasSignature = true
                                },
                                onDragEnd = {
                                    currentPath?.let { path ->
                                        signaturePaths = signaturePaths + path
                                        currentPath = null
                                    }
                                }
                            ) { change, _ ->
                                currentPath?.lineTo(change.position.x, change.position.y)
                                // We need to force recompose since Path is not observable
                                val p = currentPath
                                currentPath = null
                                currentPath = p
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        signaturePaths.forEach { path ->
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = Stroke(
                                    width = 4f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        currentPath?.let { path ->
                            drawPath(
                                path = path,
                                color = Color.Black,
                                style = Stroke(
                                    width = 4f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                if (hasSignature) {
                    TextButton(
                        onClick = {
                            signaturePaths = emptyList()
                            currentPath = null
                            hasSignature = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (isLoading) return@Button
                        val coordinates = signatureBoxCoordinates ?: return@Button
                        
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            
                            val bytes = PlatformImage.captureToPng(coordinates)
                            if (bytes != null) {
                                val effectiveKycAttemptId = if (kycAttemptId.isBlank()) {
                                    sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                                } else kycAttemptId
                                
                                viewModel.uploadSignatureFile(bytes, effectiveKycAttemptId).collect { result ->
                                    when (result) {
                                        is Resource.Success -> {
                                            isLoading = false
                                            val redirectUrl = result.data?.redirectUrl 
                                                ?: result.navigation?.getParam("redirect_url")
                                                ?: result.navigation?.getParam("esign_url")
                                            onSignatureCompleted(result.navigation?.nextScreen, redirectUrl)
                                        }
                                        is Resource.Error -> {
                                            isLoading = false
                                            errorMessage = result.message ?: "Upload failed"
                                        }
                                        is Resource.Loading -> {
                                            isLoading = true
                                        }
                                    }
                                }
                            } else {
                                isLoading = false
                                errorMessage = "Failed to capture signature"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = hasSignature && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
