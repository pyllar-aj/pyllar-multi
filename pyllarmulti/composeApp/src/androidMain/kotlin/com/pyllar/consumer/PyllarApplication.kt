package com.pyllar.consumer

import android.app.Application
import com.pyllar.consumer.di.androidPlatformModule
import com.pyllar.consumer.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PyllarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@PyllarApplication)
            modules(
                sharedModule,
                androidPlatformModule(this@PyllarApplication)
            )
        }
    }
}
