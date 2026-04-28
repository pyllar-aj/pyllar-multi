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
}
