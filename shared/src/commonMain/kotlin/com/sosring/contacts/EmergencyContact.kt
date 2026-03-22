package com.sosring.contacts

const val POLICE_NUMBER = "100"

data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val isEmergencyService: Boolean = false
)

interface ContactRepository {
    suspend fun getEmergencyContacts(): List<EmergencyContact>
    suspend fun addContact(contact: EmergencyContact)
    suspend fun removeContact(id: Long)
}
