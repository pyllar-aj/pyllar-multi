package com.pyllar.consumer.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pyllar.consumer.util.Log

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
                            val appIcon = packageManager.getApplicationIcon(packageName)
                            // Use bit manipulation to convert drawable to bitmap if toBitmap extension is not available
                            // But we can try to use the extension if we add the import
                            val bitmap = androidx.core.graphics.drawable.toBitmap(appIcon, 64, 64)
                            val iconBitmap = androidx.compose.ui.graphics.asImageBitmap(bitmap)
                            
                            upiAppsList.add(UpiAppInfo(packageName, displayName, iconBitmap))
                        } catch (e: Exception) {
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
}
