package com.sosring.android.service

import com.sosring.contacts.ContactRepository
import com.sosring.contacts.EmergencyContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryContactRepository : ContactRepository {
    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contactsFlow: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    override suspend fun getEmergencyContacts() = _contacts.value

    override suspend fun addContact(contact: EmergencyContact) {
        _contacts.value = _contacts.value + contact.copy(id = System.currentTimeMillis())
    }

    override suspend fun removeContact(id: Long) {
        _contacts.value = _contacts.value.filter { it.id != id }
    }
}
