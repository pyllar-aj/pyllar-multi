package com.pyllar.consumer.presentation.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.CoreGraphics.CGRect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    val nsUrl = remember(url) { NSURL.URLWithString(url) }
    
    if (nsUrl != null) {
        UIKitView(
            factory = {
                val config = WKWebViewConfiguration()
                val webView = WKWebView(frame = cValue<CGRect> { }, configuration = config)
                webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
                webView
            },
            modifier = modifier,
            update = { webView ->
                // Optionally handle URL changes
            }
        )
    }
}
