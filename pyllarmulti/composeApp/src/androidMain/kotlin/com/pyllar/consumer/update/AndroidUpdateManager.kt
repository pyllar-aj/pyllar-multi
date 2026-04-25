package com.pyllar.consumer.update

import android.content.Context
import com.pyllar.consumer.platform.UpdateManager
import com.pyllar.consumer.util.Log

/**
 * Android implementation of [UpdateManager].
 *
 * TODO: Add the Play App Update dependency to androidMain when ready:
 *   implementation("com.google.android.play:app-update:2.1.0")
 *   implementation("com.google.android.play:app-update-ktx:2.1.0")
 *
 * Currently uses a SharedPreferences-flag approach so the FCM-triggered
 * update check can be forwarded to MainActivity / Activity scope where
 * the full `AppUpdateManager` flow can be initiated.
 *
 * A full Play In-App Update implementation is available in the Android source at:
 *   Pyllar/android/app/src/main/java/com/pyllar/consumer/update/InAppUpdateManager.kt
 * Port it here once the Play dependency is added to build.gradle.kts androidMain.
 */
class AndroidUpdateManager(
    private val context: Context
) : UpdateManager {

    companion object {
        private const val TAG = "AndroidUpdateManager"
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_PENDING = "pending_update_check"
        private const val KEY_TIMESTAMP = "last_update_check_time"
    }

    /**
     * Sets a SharedPreferences flag so the next foreground resume in
     * MainActivity can trigger the Play In-App Update flow.
     */
    override fun scheduleUpdateCheck() {
        Log.d(TAG, "scheduleUpdateCheck: setting pending_update_check flag")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PENDING, true)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    /**
     * Returns true if MainActivity should trigger a Play update check.
     * Call [clearPendingFlag] after acting on this.
     */
    fun hasPendingUpdateCheck(): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PENDING, false)

    /** Clear the pending flag after MainActivity processes it. */
    fun clearPendingFlag() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PENDING, false)
            .apply()
    }
}
