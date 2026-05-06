package com.pyllar.consumer.util

import com.pyllar.consumer.config.IS_DEBUG

actual fun platformLog(message: String) {
    if (IS_DEBUG) {
        println(message)
    }
}
