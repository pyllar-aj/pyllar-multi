package com.pyllar.consumer.push

import android.content.Context
import com.pyllar.consumer.util.Log

/**
 * FCM token storage stub.
 *
 * TODO: Add DataStore dependency to androidMain when ready:
 *   implementation("androidx.datastore:datastore-preferences:1.1.x")
 *
 * Then replace with the full DataStore-backed implementation from:
 *   Pyllar/android/app/src/main/…/push/TokenStore.kt
 */
object TokenStore {
    private var cached: String = ""

    fun saveToken(context: Context, token: String) {
        Log.d("TokenStore", "Saving FCM token (stub): ${token.take(10)}…")
        cached = token
    }

    fun getToken(context: Context): String = cached
}
