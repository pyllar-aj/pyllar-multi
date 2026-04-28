package com.pyllar.consumer.di

import android.content.Context
import com.pyllar.consumer.domain.storage.AndroidSessionStore
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.platform.AndroidAnalyticsTracker
import com.pyllar.consumer.platform.AndroidDeviceInfoProvider
import com.pyllar.consumer.platform.AndroidPushTokenProvider
import com.pyllar.consumer.update.AndroidUpdateManager
import com.pyllar.consumer.platform.AnalyticsTracker
import com.pyllar.consumer.platform.AndroidPermissionManager
import com.pyllar.consumer.platform.DeviceInfoProvider
import com.pyllar.consumer.platform.PermissionManager
import com.pyllar.consumer.platform.PushTokenProvider
import com.pyllar.consumer.platform.UpdateManager
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific Koin module.
 * Caller should pass applicationContext when creating this module.
 *
 * Exposes platform services that shared ViewModels/screens depend on via
 * the `com.pyllar.consumer.platform.*` interfaces.
 */
fun androidPlatformModule(appContext: Context): Module = module {
    single<SessionStore> { AndroidSessionStore(appContext) }

    single<DeviceInfoProvider> { AndroidDeviceInfoProvider(appContext) }
    single<PushTokenProvider> { AndroidPushTokenProvider(appContext) }
    single<AnalyticsTracker> { AndroidAnalyticsTracker(appContext) }
    single<UpdateManager> { AndroidUpdateManager(appContext) }
    single<PermissionManager> { AndroidPermissionManager(appContext) }
    single<PlatformActions> { AndroidPlatformActions(appContext) }
}

