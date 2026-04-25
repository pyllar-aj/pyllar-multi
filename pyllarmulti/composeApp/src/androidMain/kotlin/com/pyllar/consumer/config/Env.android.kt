package com.pyllar.consumer.config

import com.pyllar.consumer.BuildConfig

/**
 * Android base URL configuration.
 *
 * Uses the Android BuildConfig so that debug/release (and any future
 * flavors) can point to different backends without changing shared code.
 */
actual fun getApiBaseUrl(): String = BuildConfig.BASE_URL

