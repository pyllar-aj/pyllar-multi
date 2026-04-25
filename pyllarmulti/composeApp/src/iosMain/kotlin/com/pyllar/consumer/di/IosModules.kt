package com.pyllar.consumer.di

import com.pyllar.consumer.domain.storage.IosSessionStore
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.platform.AnalyticsTracker
import com.pyllar.consumer.platform.DeviceInfoProvider
import com.pyllar.consumer.platform.IosAnalyticsTracker
import com.pyllar.consumer.platform.IosDeviceInfoProvider
import com.pyllar.consumer.platform.IosPermissionManager
import com.pyllar.consumer.platform.IosPushTokenProvider
import com.pyllar.consumer.platform.IosUpdateManager
import com.pyllar.consumer.platform.PermissionManager
import com.pyllar.consumer.platform.PushTokenProvider
import com.pyllar.consumer.platform.UpdateManager
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS-specific Koin module.
 *
 * Provides lightweight implementations of shared platform service
 * interfaces so that common code can depend on them without importing
 * UIKit or other iOS frameworks directly.
 */
fun iosPlatformModule(): Module = module {
    single<SessionStore> { IosSessionStore() }

    single<DeviceInfoProvider> { IosDeviceInfoProvider() }
    single<PushTokenProvider> { IosPushTokenProvider() }
    single<AnalyticsTracker> { IosAnalyticsTracker() }
    single<UpdateManager> { IosUpdateManager() }
    single<PermissionManager> { IosPermissionManager() }
}

