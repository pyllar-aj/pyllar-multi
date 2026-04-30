package com.pyllar.consumer.presentation.mutualfund.onboarding

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pyllar.consumer.util.platformLog

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun KycPlatformWebView(
    url: String,
    modifier: Modifier,
    onStatusDetected: (String) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
                }
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val interceptedUrl = request?.url?.toString()
                        platformLog("KycWebView: shouldOverrideUrlLoading: $interceptedUrl")
                        
                        if (interceptedUrl != null && (interceptedUrl.contains("pyllar.in") || interceptedUrl.contains("api.pyllar.in"))) {
                            val status = request.url.getQueryParameter("status") ?: "successful"
                            platformLog("KycWebView: Detected pyllar.in, status: $status")
                            onStatusDetected(status)
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        platformLog("KycWebView: onPageStarted: $url")
                        if (url != null && (url.contains("pyllar.in") || url.contains("api.pyllar.in"))) {
                            val uri = android.net.Uri.parse(url)
                            val status = uri.getQueryParameter("status") ?: "successful"
                            platformLog("KycWebView: Detected pyllar.in in onPageStarted, status: $status")
                            onStatusDetected(status)
                        }
                        super.onPageStarted(view, url, favicon)
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            // In KYC we usually don't want to reload the initial URL if it changes, 
            // as the WebView manages its own state, but we'll follow the pattern.
            // Actually, if 'url' changes, we might want to reload.
        },
        modifier = modifier
    )
}
