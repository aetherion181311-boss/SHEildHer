package com.sosring.sms

interface SmsService {
    suspend fun sendSms(phoneNumber: String, message: String): Boolean
}

object SmsTemplates {
    fun sosAlert(locationText: String) =
        "SOS ALERT: I need help! My location: $locationText"
    fun sosAlertPolice(locationText: String) =
        "EMERGENCY: Person in distress. Location: $locationText"
}
