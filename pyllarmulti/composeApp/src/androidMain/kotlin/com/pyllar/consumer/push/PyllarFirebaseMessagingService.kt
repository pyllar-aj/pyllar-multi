package com.pyllar.consumer.push

import android.content.Context
import com.pyllar.consumer.util.Log

/**
 * Firebase Cloud Messaging service stub — stays in androidMain.
 *
 * TODO: Add the Firebase Messaging dependency to androidMain when ready:
 *   implementation(platform("com.google.firebase:firebase-bom:33.x.x"))
 *   implementation("com.google.firebase:firebase-messaging")
 *
 * Then replace this stub with the full PyllarFirebaseMessagingService from:
 *   Pyllar/android/app/src/main/…/push/PyllarFirebaseMessagingService.kt
 *
 * The full implementation:
 *  - Stores new FCM tokens via [TokenStore]
 *  - Shows rich notifications with image download (BigPictureStyle)
 *  - Handles CHECK_UPDATE action → sets SharedPreferences flag via AndroidUpdateManager
 *  - Uses [PlatformAnalyticsLogger] for event tracking
 *
 * Until Firebase Messaging is added, this file is an empty object so the
 * package compiles cleanly.
 */
object PyllarPushStub {
    fun onNewToken(context: Context, token: String) {
        Log.d("PyllarPush", "New FCM token received (stub): $token")
    }
}
