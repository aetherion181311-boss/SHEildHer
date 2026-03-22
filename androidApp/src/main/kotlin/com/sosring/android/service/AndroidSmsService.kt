package com.sosring.android.service

import android.content.Context
import android.telephony.SmsManager
import android.os.Build
import com.sosring.sms.SmsService
import kotlinx.coroutines.delay

class AndroidSmsService(private val context: Context) : SmsService {
    override suspend fun sendSms(phoneNumber: String, message: String): Boolean {
        repeat(3) { attempt ->
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION") SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                return true
            } catch (e: Exception) {
                delay(2_000L * (attempt + 1))
            }
        }
        return false
    }
}
