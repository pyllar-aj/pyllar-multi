package com.pyllar.consumer.util

import platform.Foundation.NSLog

actual fun platformLog(message: String) {
    println(message)
}
