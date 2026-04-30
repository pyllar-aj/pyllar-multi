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

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun KycPlatformWebView(
    url: String,
    modifier: Modifier,
    onStatusDetected: (String) -> Unit
) {
    val nsUrl = remember(url) { NSURL.URLWithString(url) }
    val navigationDelegate = remember { 
        KycNavigationDelegate(onStatusDetected)
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

private class KycNavigationDelegate(
    private val onStatusDetected: (String) -> Unit
) : NSObject(), WKNavigationDelegateProtocol {
    
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit
    ) {
        val url = decidePolicyForNavigationAction.request.URL?.absoluteString
        platformLog("KycWebView: decidePolicyForNavigationAction: $url")
        
        if (url != null && (url.contains("pyllar.in") || url.contains("api.pyllar.in"))) {
            val status = parseStatus(url)
            platformLog("KycWebView: Detected pyllar.in, status: $status")
            onStatusDetected(status)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        } else {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        }
    }

    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        val url = webView.URL?.absoluteString
        platformLog("KycWebView: didStartProvisionalNavigation: $url")
        if (url != null && (url.contains("pyllar.in") || url.contains("api.pyllar.in"))) {
            val status = parseStatus(url)
            platformLog("KycWebView: Detected pyllar.in in didStart, status: $status")
            onStatusDetected(status)
        }
    }

    private fun parseStatus(url: String): String {
        val nsUrl = NSURL.URLWithString(url) ?: return "successful"
        val components = NSURLComponents.componentsWithURL(nsUrl, false)
        val queryItems = components?.queryItems ?: return "successful"
        
        for (item in queryItems) {
            val queryItem = item as? NSURLQueryItem
            if (queryItem?.name == "status") {
                return queryItem.value ?: "successful"
            }
        }
        return "successful"
    }
}
