package com.pyllar.consumer

import androidx.compose.ui.window.ComposeUIViewController
import com.pyllar.consumer.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}