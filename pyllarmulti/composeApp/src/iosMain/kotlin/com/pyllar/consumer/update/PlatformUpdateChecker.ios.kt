package com.pyllar.consumer.update

import com.pyllar.consumer.navigation.ForceUpdateManager
import com.pyllar.consumer.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.datetime.Clock
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

private const val TAG = "PlatformUpdateChecker"
private const val BUNDLE_ID = "com.pyllar.consumer"
private const val FALLBACK_TRACK_ID = 6767513475L
private const val PREF_OPTIONAL_DISMISSED_AT = "pyllar_optional_update_dismissed_at"
private const val THROTTLE_SECONDS = 72 * 60 * 60.0 // 72 hours

private val json = Json { ignoreUnknownKeys = true }

actual suspend fun checkPlatformForUpdates(manager: ForceUpdateManager) {
    val installed = installedVersion() ?: run {
        Log.w(TAG, "Could not read CFBundleShortVersionString")
        return
    }
    Log.d(TAG, "Installed version: $installed")

    val (storeVersion, trackId) = fetchStoreVersion() ?: run {
        Log.w(TAG, "App Store version fetch failed or app not on store")
        return
    }
    Log.d(TAG, "App Store version: $storeVersion  trackId: $trackId")

    if (compareVersions(storeVersion, installed) <= 0) {
        Log.d(TAG, "Already on latest version, no update needed")
        return
    }

    val storePatch = storeVersion.split(".").getOrNull(2)?.toIntOrNull() ?: 0
    val storeDeepLink = "itms-apps://itunes.apple.com/app/id$trackId"
    val storeWebUrl = "https://apps.apple.com/app/id$trackId"

    if (storePatch > 90) {
        Log.d(TAG, "Force update required (patch=$storePatch > 90)")
        manager.setForceUpdate(updateUrl = storeDeepLink, webUrl = storeWebUrl, message = null)
    } else if (isOptionalUpdateThrottled()) {
        Log.d(TAG, "Optional update available but throttled (dismissed within 72 h)")
    } else {
        Log.d(TAG, "Optional update available (patch=$storePatch)")
        manager.setOptionalUpdate(storeUrl = storeDeepLink)
    }
}

actual fun onOptionalUpdateDismissed() {
    val nowSeconds = Clock.System.now().epochSeconds.toDouble()
    NSUserDefaults.standardUserDefaults.setDouble(nowSeconds, forKey = PREF_OPTIONAL_DISMISSED_AT)
    NSUserDefaults.standardUserDefaults.synchronize()
}

private fun isOptionalUpdateThrottled(): Boolean {
    val lastDismissed = NSUserDefaults.standardUserDefaults.doubleForKey(PREF_OPTIONAL_DISMISSED_AT)
    if (lastDismissed == 0.0) return false
    val elapsed = Clock.System.now().epochSeconds.toDouble() - lastDismissed
    return elapsed < THROTTLE_SECONDS
}

private fun installedVersion(): String? =
    NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String

private suspend fun fetchStoreVersion(): Pair<String, Long>? {
    val client = HttpClient(Darwin)
    return try {
        val body = client.get("https://itunes.apple.com/lookup?bundleId=$BUNDLE_ID").bodyAsText()
        val root = json.parseToJsonElement(body).jsonObject
        val results = root["results"]?.jsonArray ?: return null
        if (results.isEmpty()) return null
        val app = results[0].jsonObject
        val version = app["version"]?.jsonPrimitive?.content ?: return null
        val trackId = app["trackId"]?.jsonPrimitive?.longOrNull ?: FALLBACK_TRACK_ID
        Pair(version, trackId)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch App Store version: ${e.message}")
        null
    } finally {
        client.close()
    }
}

/** Returns positive if v1 > v2, zero if equal, negative if v1 < v2. */
private fun compareVersions(v1: String, v2: String): Int {
    val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
    val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(p1.size, p2.size)) {
        val diff = (p1.getOrElse(i) { 0 }) - (p2.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}
