package com.pyllar.consumer.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.pyllar.consumer.util.Log
import android.app.Activity
import android.content.ContextWrapper
import com.google.android.play.core.review.ReviewManagerFactory
import com.pyllar.consumer.analytics.AnalyticsLogger


class AndroidPlatformActions(private val context: Context) : PlatformActions {

    override fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AndroidPlatformActions", "Failed to open URL: $url", e)
        }
    }

    override fun openUpiUrl(url: String, packageName: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                if (packageName != null) {
                    setPackage(packageName)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AndroidPlatformActions", "Failed to open UPI URL: $url with package: $packageName", e)
            // Fallback to generic open
            openUrl(url)
        }
    }

    override fun shareText(text: String, title: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("AndroidPlatformActions", "Failed to share text", e)
        }
    }

    override fun openWhatsApp(phoneNumber: String, message: String) {
        val encodedMessage = Uri.encode(message)
        val whatsappUrl = "https://wa.me/$phoneNumber?text=$encodedMessage"
        
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(whatsappUrl)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to web if WhatsApp app not installed
            openUrl(whatsappUrl)
        } catch (e: Exception) {
            Log.e("AndroidPlatformActions", "Failed to open WhatsApp", e)
            openUrl(whatsappUrl)
        }
    }

    override fun getInstalledUpiApps(): List<UpiAppInfo> {
        val packageManager = context.packageManager
        val upiIntents = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay")),
            Intent(Intent.ACTION_VIEW, Uri.parse("upi://mandate"))
        )
        
        val upiAppsSet = mutableSetOf<String>()
        val upiAppsList = mutableListOf<UpiAppInfo>()
        val blacklist = setOf("com.pyllar.consumer", "com.pyllar.consumer.debug")

        upiIntents.forEach { intent ->
            try {
                val activities = packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                activities.forEach { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    if (packageName in blacklist) return@forEach
                    
                    if (!upiAppsSet.contains(packageName)) {
                        upiAppsSet.add(packageName)
                        val displayName = try {
                            resolveInfo.loadLabel(packageManager).toString()
                        } catch (e: Exception) {
                            packageName
                        }
                        
                        try {
                            val appIcon: Drawable = packageManager.getApplicationIcon(packageName)
                            // Use core-ktx extension to convert drawable to bitmap
                            val bitmap = appIcon.toBitmap(128, 128)
                            val iconBitmap = bitmap.asImageBitmap()
                            
                            upiAppsList.add(UpiAppInfo(packageName, displayName, iconBitmap))
                        } catch (e: Exception) {
                            Log.e("AndroidPlatformActions", "Failed to load icon for $packageName", e)
                            upiAppsList.add(UpiAppInfo(packageName, displayName, null))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AndroidPlatformActions", "Error querying UPI apps", e)
            }
        }
        return upiAppsList.sortedBy { it.displayName }
    }

    override fun openAppSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AndroidPlatformActions", "Failed to open app settings", e)
        }
    }

    override fun generateReferralLink(referrerId: String, onComplete: (String?) -> Unit) {
        com.pyllar.consumer.analytics.AppsFlyerTracker.generateReferralLink(context, referrerId, onComplete)
    }

    override fun requestInAppReview(
        screenName: String,
        silentFallback: Boolean,
        trigger: String
    ) {

        Log.d("AndroidPlatformActions", "🚀 requestInAppReview called (silentFallback=$silentFallback)")
        AnalyticsLogger.logEvent(
            context,
            "rate_us_in_app_review_started",
            mapOf("screen_name" to screenName, "silent" to silentFallback, "trigger" to trigger)
        )
        val activity = context.findActivity() ?: run {
            Log.e("AndroidPlatformActions", "❌ Could not find Activity from Context, falling back to Play Store")
            AnalyticsLogger.logEvent(
                context,
                "rate_us_in_app_review_fallback",
                mapOf("reason" to "no_activity", "screen_name" to screenName, "trigger" to trigger)
            )
            if (!silentFallback) openPlayStore()
            return
        }

        Log.d("AndroidPlatformActions", "✅ Found Activity, requesting review flow")
        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        val startTime = System.currentTimeMillis()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("AndroidPlatformActions", "✅ Review flow request successful, launching review")
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { flowTask ->
                    val elapsedTime = System.currentTimeMillis() - startTime
                    Log.d("AndroidPlatformActions", "✅ Review flow completed (took ${elapsedTime}ms)")

                    if (elapsedTime < 1000) {
                        Log.w("AndroidPlatformActions", "⚠️ Review flow completed too quickly, likely no dialog shown.")
                        AnalyticsLogger.logEvent(
                            context,
                            "rate_us_in_app_review_fallback",
                            mapOf("reason" to "dialog_not_shown", "elapsed_ms" to elapsedTime, "screen_name" to screenName, "trigger" to trigger)
                        )
                        if (!silentFallback) openPlayStore()
                    } else {
                        Log.d("AndroidPlatformActions", "✅ Review flow likely showed dialog (took ${elapsedTime}ms)")
                        AnalyticsLogger.logEvent(
                            context,
                            "rate_us_in_app_review_shown",
                            mapOf("elapsed_ms" to elapsedTime, "screen_name" to screenName, "trigger" to trigger)
                        )
                    }
                }
            } else {
                Log.e("AndroidPlatformActions", "❌ Review flow request failed: ${task.exception?.message}")
                AnalyticsLogger.logEvent(
                    context,
                    "rate_us_in_app_review_fallback",
                    mapOf("reason" to "request_failed", "error" to (task.exception?.message ?: "unknown"), "screen_name" to screenName, "trigger" to trigger)
                )
                if (!silentFallback) openPlayStore()
            }
        }
    }

    private fun openPlayStore() {
        try {
            val packageName = context.packageName
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(playStoreIntent)
                Log.d("AndroidPlatformActions", "✅ Opened Play Store app")
            } catch (e: ActivityNotFoundException) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Log.d("AndroidPlatformActions", "✅ Opened Play Store in browser")
            }
        } catch (e: Exception) {
            Log.e("AndroidPlatformActions", "❌ Could not open Play Store: ${e.message}")
        }
    }

    private fun Context.findActivity(): Activity? {
        var current = this
        while (current is ContextWrapper) {
            if (current is Activity) {
                return current
            }
            current = current.baseContext
        }
        return null
    }

    override fun playRedemptionSuccessSound() {
        try {
            val resId = context.resources.getIdentifier("redemption_success", "raw", context.packageName)
            if (resId != 0) {
                val mp = android.media.MediaPlayer.create(context, resId)
                mp?.start()
                mp?.setOnCompletionListener { it.release() }
            } else {
                Log.w("AndroidPlatformActions", "redemption_success raw resource not found")
            }
        } catch (e: Exception) {
            Log.e("AndroidPlatformActions", "Failed to play redemption success sound", e)
        }
    }
}

