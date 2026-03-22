package com.sosring.android

import android.app.Application
import android.content.Intent
import com.sosring.android.service.SosRingService

class SosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startForegroundService(Intent(this, SosRingService::class.java))
    }
}
