package com.pyllar.consumer.di

import org.koin.core.context.startKoin

/**
 * Initializes Koin on iOS.
 *
 * The iOS host app must call this before any shared ViewModels are created.
 * This is safe to call multiple times.
 */
private var koinStarted: Boolean = false

fun initKoin() {
    if (koinStarted) return
    startKoin {
        modules(
            sharedModule,
            iosPlatformModule()
        )
    }
    koinStarted = true
}

