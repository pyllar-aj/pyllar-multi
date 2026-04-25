package com.pyllar.consumer.util

import android.util.Log

actual fun platformLog(message: String) {
    Log.d("Pyllar", message)
}

