package com.pyllar.consumer.update

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.*
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.pyllar.consumer.BuildConfig
import com.pyllar.consumer.analytics.AnalyticsLogger
import com.pyllar.consumer.util.Log

class InAppUpdateManager(private val context: Context) {

    private val appUpdateManager: AppUpdateManager? by lazy {
        try {
            if (!BuildConfig.DEBUG) AppUpdateManagerFactory.create(context) else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AppUpdateManager", e)
            null
        }
    }

    var immediateUpdateAvailable by mutableStateOf(false)
        private set

    var flexibleUpdateAvailable by mutableStateOf(false)
        private set

    var flexibleUpdateDownloading by mutableStateOf(false)
        private set

    var flexibleUpdateReady by mutableStateOf(false)
        private set

    private var _currentUpdateInfo: AppUpdateInfo? = null

    companion object {
        private const val TAG = "InAppUpdateManager"
        const val UPDATE_REQUEST_CODE = 1001
    }

    fun checkForUpdate(
        activity: Activity,
        onImmediateUpdateAvailable: () -> Unit = {},
        onFlexibleUpdateAvailable: () -> Unit = {},
        onNoUpdateAvailable: () -> Unit = {},
        onForceUpdateRequired: () -> Unit = {}
    ) {
        if (BuildConfig.DEBUG) {
            onNoUpdateAvailable()
            return
        }

        val updateManager = appUpdateManager ?: run {
            onNoUpdateAvailable()
            return
        }

        try {
            updateManager.appUpdateInfo.addOnSuccessListener { updateInfo ->
                _currentUpdateInfo = updateInfo
                val availability = updateInfo.updateAvailability()
                val isImmediateAllowed = updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                val isFlexibleAllowed = updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                val availableCode = updateInfo.availableVersionCode()

                when {
                    availability == UpdateAvailability.UPDATE_AVAILABLE && isForceUpdateVersion(availableCode) -> {
                        AnalyticsLogger.logEvent(context, "update_force_required", mapOf("available_version_code" to availableCode))
                        onForceUpdateRequired()
                    }
                    availability == UpdateAvailability.UPDATE_AVAILABLE && isImmediateAllowed -> {
                        immediateUpdateAvailable = true
                        onImmediateUpdateAvailable()
                    }
                    availability == UpdateAvailability.UPDATE_AVAILABLE && isFlexibleAllowed -> {
                        flexibleUpdateAvailable = true
                        onFlexibleUpdateAvailable()
                    }
                    availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        if (isImmediateAllowed) {
                            immediateUpdateAvailable = true
                            onImmediateUpdateAvailable()
                        } else if (isFlexibleAllowed) {
                            startFlexibleUpdate(activity)
                        }
                    }
                    else -> {
                        flexibleUpdateAvailable = false
                        onNoUpdateAvailable()
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check for updates", exception)
                flexibleUpdateAvailable = false
                onNoUpdateAvailable()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while checking for updates", e)
            flexibleUpdateAvailable = false
            onNoUpdateAvailable()
        }
    }

    fun isForceUpdateVersion(availableVersionCode: Int): Boolean {
        val patch = availableVersionCode % 100
        return patch > 90
    }

    fun startImmediateUpdate(activity: Activity) {
        val updateManager = appUpdateManager ?: run { immediateUpdateAvailable = false; return }
        val updateInfo = _currentUpdateInfo ?: run {
            Log.e(TAG, "Cannot start IMMEDIATE update: no update info available")
            immediateUpdateAvailable = false
            return
        }

        if (!updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            immediateUpdateAvailable = false
            return
        }

        immediateUpdateAvailable = false
        AnalyticsLogger.logEvent(context, "update_started", mapOf("update_type" to "immediate"))

        try {
            updateManager.startUpdateFlowForResult(updateInfo, AppUpdateType.IMMEDIATE, activity, UPDATE_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start IMMEDIATE update", e)
            immediateUpdateAvailable = false
            AnalyticsLogger.logEvent(context, "update_start_failed", mapOf("update_type" to "immediate", "error" to (e.message ?: "unknown")))
        }
    }

    fun startFlexibleUpdate(activity: Activity) {
        val updateManager = appUpdateManager ?: run { flexibleUpdateDownloading = false; return }
        val updateInfo = _currentUpdateInfo ?: run {
            Log.e(TAG, "Cannot start FLEXIBLE update: no update info available")
            flexibleUpdateDownloading = false
            return
        }

        if (!updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            flexibleUpdateDownloading = false
            return
        }

        flexibleUpdateDownloading = true
        AnalyticsLogger.logEvent(context, "update_started", mapOf("update_type" to "flexible"))

        try {
            updateManager.startUpdateFlowForResult(updateInfo, AppUpdateType.FLEXIBLE, activity, UPDATE_REQUEST_CODE)

            try {
                updateManager.registerListener { state ->
                    when (state.installStatus()) {
                        com.google.android.play.core.install.model.InstallStatus.DOWNLOADED -> {
                            flexibleUpdateDownloading = false
                            flexibleUpdateReady = true
                            AnalyticsLogger.logEvent(context, "update_downloaded", mapOf("update_type" to "flexible"))
                        }
                        com.google.android.play.core.install.model.InstallStatus.FAILED -> {
                            flexibleUpdateDownloading = false
                            flexibleUpdateReady = false
                            AnalyticsLogger.logEvent(context, "update_failed", mapOf("update_type" to "flexible"))
                        }
                        com.google.android.play.core.install.model.InstallStatus.CANCELED -> {
                            flexibleUpdateDownloading = false
                            flexibleUpdateReady = false
                            AnalyticsLogger.logEvent(context, "update_canceled", mapOf("update_type" to "flexible"))
                        }
                        else -> { flexibleUpdateDownloading = true }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register update listener", e)
                flexibleUpdateDownloading = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FLEXIBLE update", e)
            flexibleUpdateDownloading = false
            AnalyticsLogger.logEvent(context, "update_start_failed", mapOf("update_type" to "flexible", "error" to (e.message ?: "unknown")))
        }
    }

    fun completeFlexibleUpdate() {
        val updateManager = appUpdateManager ?: run { flexibleUpdateReady = false; flexibleUpdateAvailable = false; return }
        if (!flexibleUpdateReady) return

        AnalyticsLogger.logEvent(context, "update_completing", mapOf("update_type" to "flexible"))
        try {
            updateManager.completeUpdate()
            flexibleUpdateReady = false
            flexibleUpdateAvailable = false
            AnalyticsLogger.logEvent(context, "update_completed", mapOf("update_type" to "flexible", "status" to "success"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete FLEXIBLE update", e)
            flexibleUpdateReady = false
            flexibleUpdateAvailable = false
            AnalyticsLogger.logEvent(context, "update_completed", mapOf("update_type" to "flexible", "status" to "failed"))
        }
    }

    fun checkUpdateStatus(
        onUpdateReady: () -> Unit = {},
        onNoUpdateReady: () -> Unit = {}
    ) {
        if (BuildConfig.DEBUG) { onNoUpdateReady(); return }
        val updateManager = appUpdateManager ?: run { onNoUpdateReady(); return }

        try {
            updateManager.appUpdateInfo.addOnSuccessListener { updateInfo ->
                if (updateInfo.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
                    flexibleUpdateReady = true
                    flexibleUpdateDownloading = false
                    onUpdateReady()
                } else {
                    flexibleUpdateReady = false
                    onNoUpdateReady()
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check update status", exception)
                onNoUpdateReady()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while checking update status", e)
            onNoUpdateReady()
        }
    }

    fun handleUpdateResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                AnalyticsLogger.logEvent(context, "update_flow_completed", mapOf("result" to "success"))
            }
            Activity.RESULT_CANCELED -> {
                flexibleUpdateDownloading = false
                AnalyticsLogger.logEvent(context, "update_flow_completed", mapOf("result" to "canceled"))
            }
            com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                flexibleUpdateDownloading = false
                flexibleUpdateReady = false
                AnalyticsLogger.logEvent(context, "update_flow_completed", mapOf("result" to "failed"))
            }
        }
    }

    fun dismissFlexibleUpdate() { flexibleUpdateAvailable = false }
    fun dismissImmediateUpdate() { immediateUpdateAvailable = false }
}
