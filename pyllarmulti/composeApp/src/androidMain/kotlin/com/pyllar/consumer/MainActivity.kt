package com.pyllar.consumer

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pyllar.consumer.analytics.AnalyticsLogger
import com.pyllar.consumer.navigation.ForceUpdateManager
import com.pyllar.consumer.platform.AndroidPermissionManager
import com.pyllar.consumer.presentation.components.ImmediateUpdateBottomSheet
import com.pyllar.consumer.presentation.components.UpdateBottomSheet
import com.pyllar.consumer.presentation.components.UpdateReadyBottomSheet
import com.pyllar.consumer.update.InAppUpdateManager
import com.pyllar.consumer.util.InstallReferrerHelper
import com.pyllar.consumer.util.Log
import org.koin.android.ext.android.get
import org.koin.compose.KoinContext

class MainActivity : AppCompatActivity() {

    companion object {
        var instance: MainActivity? = null
            private set
    }

    private val notifPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        get<AndroidPermissionManager>().onNotificationResult(granted)
    }
    private val locationPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        get<AndroidPermissionManager>().onLocationResult(granted)
    }

    private val forceUpdateManager: ForceUpdateManager by lazy { get() }
    private val inAppUpdateManager: InAppUpdateManager by lazy { get() }

    // Activity-level Compose state — set from non-composable callbacks, observed in setContent
    private val showImmediateUpdateSheet = mutableStateOf(false)
    private val showFlexibleUpdateSheet = mutableStateOf(false)
    private val showUpdateReadySheet = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableEdgeToEdge()

        get<AndroidPermissionManager>().setLaunchers(notifPermissionLauncher, locationPermissionLauncher)

        // Capture UTM install referrer params on first launch
        InstallReferrerHelper.captureIfNeeded(this)

        // Handle FCM notification action from the launching intent
        checkForUpdateIfNeeded(intent)
        checkForPendingUpdateCheck()
        checkForUpdateOnStartup()

        setContent {
            KoinContext {
                Box(Modifier.fillMaxSize()) {
                    // ForceUpdateDialog is rendered inside App() for both platforms.
                    App()

                    // Sync inAppUpdateManager state → local sheet flags
                    val immediateAvailable = inAppUpdateManager.immediateUpdateAvailable
                    val flexibleAvailable = inAppUpdateManager.flexibleUpdateAvailable
                    val flexibleReady = inAppUpdateManager.flexibleUpdateReady

                    LaunchedEffect(immediateAvailable) {
                        if (immediateAvailable && canShowUpdateBottomSheet()) {
                            showImmediateUpdateSheet.value = true
                        } else if (!immediateAvailable) {
                            showImmediateUpdateSheet.value = false
                        }
                    }
                    LaunchedEffect(flexibleAvailable) {
                        if (flexibleAvailable && canShowUpdateBottomSheet()) {
                            showFlexibleUpdateSheet.value = true
                        } else if (!flexibleAvailable) {
                            showFlexibleUpdateSheet.value = false
                        }
                    }
                    LaunchedEffect(flexibleReady) {
                        if (flexibleReady) showUpdateReadySheet.value = true
                    }

                    if (showImmediateUpdateSheet.value && immediateAvailable) {
                        ImmediateUpdateBottomSheet(
                            onUpdateClick = {
                                AnalyticsLogger.logEvent(this@MainActivity, "update_immediate_clicked", mapOf("update_type" to "immediate"))
                                inAppUpdateManager.startImmediateUpdate(this@MainActivity)
                                showImmediateUpdateSheet.value = false
                            },
                            onDismiss = {
                                AnalyticsLogger.logEvent(this@MainActivity, "update_immediate_dismissed", mapOf("update_type" to "immediate"))
                                inAppUpdateManager.dismissImmediateUpdate()
                                showImmediateUpdateSheet.value = false
                                setNextShowTime()
                            }
                        )
                    }

                    if (showFlexibleUpdateSheet.value && flexibleAvailable) {
                        UpdateBottomSheet(
                            onUpdateClick = {
                                AnalyticsLogger.logEvent(this@MainActivity, "update_flexible_clicked", mapOf("update_type" to "flexible"))
                                inAppUpdateManager.startFlexibleUpdate(this@MainActivity)
                            },
                            onDismiss = {
                                AnalyticsLogger.logEvent(this@MainActivity, "update_flexible_dismissed", mapOf("update_type" to "flexible"))
                                inAppUpdateManager.dismissFlexibleUpdate()
                                showFlexibleUpdateSheet.value = false
                                setNextShowTime()
                            }
                        )
                    }

                    if (showUpdateReadySheet.value && flexibleReady) {
                        UpdateReadyBottomSheet(
                            onRestartClick = {
                                AnalyticsLogger.logEvent(this@MainActivity, "update_flexible_restart_clicked", mapOf("update_type" to "flexible"))
                                inAppUpdateManager.completeFlexibleUpdate()
                            },
                            onDismiss = {
                                showUpdateReadySheet.value = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkForUpdateIfNeeded(intent)
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateManager.checkUpdateStatus(
            onUpdateReady = {
                Log.d("MainActivity", "FLEXIBLE update ready - showing restart prompt")
                showUpdateReadySheet.value = true
            }
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == InAppUpdateManager.UPDATE_REQUEST_CODE) {
            inAppUpdateManager.handleUpdateResult(resultCode)
        }
    }

    private fun checkForUpdateIfNeeded(intent: Intent) {
        val action = intent.getStringExtra("action") ?: return
        when (action) {
            "FORCE_UPDATE" -> {
                val updateUrl = intent.getStringExtra("url")
                val body = intent.getStringExtra("body")
                forceUpdateManager.setForceUpdate(updateUrl = updateUrl, webUrl = null, message = body)
                Log.d("MainActivity", "FORCE_UPDATE action from notification")
            }
            "CHECK_UPDATE" -> {
                if (!canShowUpdateBottomSheet()) {
                    AnalyticsLogger.logEvent(this, "update_check_throttled", mapOf("source" to "notification_click", "reason" to "24_hour_restriction"))
                    return
                }
                AnalyticsLogger.logEvent(this, "update_check_triggered", mapOf("source" to "notification_click"))
                inAppUpdateManager.checkForUpdate(
                    activity = this,
                    onForceUpdateRequired = {
                        forceUpdateManager.setForceUpdate(updateUrl = null, webUrl = null, message = null)
                    },
                    onImmediateUpdateAvailable = {
                        AnalyticsLogger.logEvent(this, "update_available", mapOf("update_type" to "immediate", "source" to "notification_click"))
                        showImmediateUpdateSheet.value = true
                    },
                    onFlexibleUpdateAvailable = {
                        AnalyticsLogger.logEvent(this, "update_available", mapOf("update_type" to "flexible", "source" to "notification_click"))
                        showFlexibleUpdateSheet.value = true
                    },
                    onNoUpdateAvailable = {
                        AnalyticsLogger.logEvent(this, "update_not_available", mapOf("source" to "notification_click"))
                    }
                )
            }
        }
    }

    private fun checkForPendingUpdateCheck() {
        val prefs = getSharedPreferences("update_prefs", MODE_PRIVATE)
        val pendingCheck = prefs.getBoolean("pending_update_check", false)
        val lastCheckTime = prefs.getLong("last_update_check_time", 0)
        val nextShowTime = prefs.getLong("next_show_time", 0)
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val canShow = nextShowTime == 0L || now >= nextShowTime

        if (pendingCheck && (now - lastCheckTime) >= oneDayMs && canShow) {
            prefs.edit().putLong("last_update_check_time", now).apply()
            AnalyticsLogger.logEvent(this, "update_check_triggered", mapOf("source" to "pending_check"))
            inAppUpdateManager.checkForUpdate(
                activity = this,
                onForceUpdateRequired = { forceUpdateManager.setForceUpdate(null, null, null) },
                onImmediateUpdateAvailable = {
                    AnalyticsLogger.logEvent(this, "update_available", mapOf("update_type" to "immediate", "source" to "pending_check"))
                    showImmediateUpdateSheet.value = true
                },
                onFlexibleUpdateAvailable = {
                    AnalyticsLogger.logEvent(this, "update_available", mapOf("update_type" to "flexible", "source" to "pending_check"))
                    showFlexibleUpdateSheet.value = true
                },
                onNoUpdateAvailable = {
                    prefs.edit().putBoolean("pending_update_check", false).apply()
                }
            )
        }
    }

    /**
     * On every launch, checks Play Store for an available update - force (patch > 90),
     * or optional immediate/flexible - independent of any FCM push.
     * Skipped within the first 30 hours after install to avoid blocking fresh-install users.
     */
    private fun checkForUpdateOnStartup() {
        try {
            val pkgInfo = packageManager.getPackageInfo(packageName, 0)
            val millisSinceInstall = pkgInfo.lastUpdateTime - pkgInfo.firstInstallTime
            if (millisSinceInstall < (30 * 60 * 60 * 1000L)) {
                Log.d("MainActivity", "Skipping update check - fresh install")
                return
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Could not read package info: ${e.message}")
        }

        inAppUpdateManager.checkForUpdate(
            activity = this,
            onForceUpdateRequired = {
                Log.d("MainActivity", "FORCE update required on startup (patch > 90)")
                forceUpdateManager.setForceUpdate(updateUrl = null, webUrl = null, message = null)
            },
            onImmediateUpdateAvailable = {
                if (canShowUpdateBottomSheet()) {
                    AnalyticsLogger.logEvent(this, "update_available", mapOf("update_type" to "immediate", "source" to "app_startup"))
                    showImmediateUpdateSheet.value = true
                }
            },
            onFlexibleUpdateAvailable = {
                if (canShowUpdateBottomSheet()) {
                    AnalyticsLogger.logEvent(this, "update_available", mapOf("update_type" to "flexible", "source" to "app_startup"))
                    showFlexibleUpdateSheet.value = true
                }
            }
        )
    }

    private fun setNextShowTime() {
        val nextShowTime = System.currentTimeMillis() + (10 * 24 * 60 * 60 * 1000L)
        getSharedPreferences("update_prefs", MODE_PRIVATE).edit()
            .putLong("next_show_time", nextShowTime)
            .apply()
    }

    private fun canShowUpdateBottomSheet(): Boolean {
        val prefs = getSharedPreferences("update_prefs", MODE_PRIVATE)
        val nextShowTime = prefs.getLong("next_show_time", 0)
        return nextShowTime == 0L || System.currentTimeMillis() >= nextShowTime
    }
}
