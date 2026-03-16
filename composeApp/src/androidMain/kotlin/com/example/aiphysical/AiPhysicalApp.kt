package com.example.aiphysical

import android.app.Application
import android.content.Context

/**
 * Application-class: provides a static application context that can be accessed
 * from platform-specific service implementations (e.g. for reading assets).
 */
class AiPhysicalApp : Application() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}

