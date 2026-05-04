package com.pyllar.consumer.presentation.mutualfund.onboarding

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val ESIGN_CALLBACK_HOST = "ogc7cj4zsk.execute-api.ap-south-1.amazonaws.com"
private const val TAG = "EsignPlatformWebView"

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun EsignPlatformWebView(
    url: String,
    modifier: Modifier,
    onEsignComplete: () -> Unit,
    onEsignCancel: () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportMultipleWindows(false)
                    javaScriptCanOpenWindowsAutomatically = false
                    
                    val defaultUserAgent = userAgentString
                    userAgentString = defaultUserAgent.replace("Mobile", "").replace("mobile", "") + " Mobile"
                    
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    textZoom = 100
                    defaultFontSize = 16
                    minimumFontSize = 12
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    allowFileAccess = true
                    allowContentAccess = true
                    loadsImagesAutomatically = true
                }

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // ✅ CRITICAL PATCH: Fix Layout (Iframe height + Footer overlapping)
                        view?.evaluateJavascript(
                            """
                            (function() {
                                var preview = document.querySelector('.preview');
                                if (preview) {
                                    preview.style.height = 'auto';
                                    preview.style.minHeight = '70vh';
                                    preview.style.display = 'block';
                                }
                                var iframe = document.getElementById('pdf-frame') || document.querySelector('iframe');
                                if (iframe) {
                                    iframe.style.height = '160vh';
                                    iframe.style.minHeight = '1400px';
                                    if (iframe.parentElement) {
                                        iframe.parentElement.style.height = 'auto';
                                        iframe.parentElement.style.minHeight = '1400px';
                                    }
                                }
                                var footer = document.querySelector('.footer');
                                if (footer) {
                                    footer.style.marginTop = '20px';
                                    footer.style.position = 'relative'; 
                                }
                                return "Layout Fixed";
                            })();
                            """.trimIndent(), null
                        )
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val targetUrl = request?.url?.toString().orEmpty()
                        if (isEsignCompletionUrl(targetUrl)) {
                            onEsignComplete()
                            return true
                        }
                        return false
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        val targetUrl = url.orEmpty()
                        if (isEsignCompletionUrl(targetUrl)) {
                            onEsignComplete()
                            return true
                        }
                        return false
                    }
                }

                loadUrl(url)
            }
        },
        update = { /* No-op */ }
    )
}

private fun isEsignCompletionUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return try {
        val uri = Uri.parse(url)
        val hostMatch = uri.host?.equals(ESIGN_CALLBACK_HOST, ignoreCase = true) == true
        hostMatch || url.startsWith("https://$ESIGN_CALLBACK_HOST", ignoreCase = true)
    } catch (e: Exception) {
        false
    }
}
