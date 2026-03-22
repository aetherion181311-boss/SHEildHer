package com.sosring.android

import android.app.Application

class SosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Service is started from MainActivity after permissions are granted
    }
}
