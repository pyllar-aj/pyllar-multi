package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.WebKit.*
import platform.Foundation.*
import platform.CoreGraphics.CGRect
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import com.pyllar.consumer.util.platformLog

private const val ESIGN_CALLBACK_HOST = "ogc7cj4zsk.execute-api.ap-south-1.amazonaws.com"

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun EsignPlatformWebView(
    url: String,
    modifier: Modifier,
    onEsignComplete: () -> Unit,
    onEsignCancel: () -> Unit
) {
    val nsUrl = remember(url) { NSURL.URLWithString(url) }
    val navigationDelegate = remember { 
        EsignNavigationDelegate(onEsignComplete)
    }

    if (nsUrl != null) {
        UIKitView(
            factory = {
                val config = WKWebViewConfiguration()
                val webView = WKWebView(frame = cValue<CGRect> { }, configuration = config)
                webView.navigationDelegate = navigationDelegate
                webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
                webView
            },
            modifier = modifier,
            update = { webView ->
                // No-op
            }
        )
    }
}

private class EsignNavigationDelegate(
    private val onEsignComplete: () -> Unit
) : NSObject(), WKNavigationDelegateProtocol {
    
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit
    ) {
        val url = decidePolicyForNavigationAction.request.URL?.absoluteString
        platformLog("EsignWebView: decidePolicyForNavigationAction: $url")
        
        if (url != null && isEsignCompletionUrl(url)) {
            platformLog("EsignWebView: Detected completion host: $ESIGN_CALLBACK_HOST")
            onEsignComplete()
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        } else {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        }
    }

    private fun isEsignCompletionUrl(url: String): Boolean {
        return url.contains(ESIGN_CALLBACK_HOST, ignoreCase = true)
    }
}
