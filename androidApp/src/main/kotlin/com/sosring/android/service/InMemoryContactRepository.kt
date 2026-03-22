package com.sosring.android.service

import com.sosring.contacts.ContactRepository
import com.sosring.contacts.EmergencyContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryContactRepository : ContactRepository {
    private val contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())

    override suspend fun getEmergencyContacts() = contacts.value
    override suspend fun addContact(contact: EmergencyContact) {
        contacts.value = contacts.value + contact.copy(id = System.currentTimeMillis())
    }
    override suspend fun removeContact(id: Long) {
        contacts.value = contacts.value.filter { it.id != id }
    }
}
