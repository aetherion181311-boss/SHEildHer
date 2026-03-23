package com.sosring.android.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sosring.android.MainActivity

class SosRingService : Service() {
    companion object { const val CHANNEL_ID = "sos_ring_service" }

    override fun onCreate() {
        super.onCreate()
        try {
            createChannel()
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SOS Ring")
                .setContentText("Monitoring for SOS trigger…")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(
                    PendingIntent.getActivity(this, 0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE)
                )
                .build()
            startForeground(1, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "SOS Ring Service",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
