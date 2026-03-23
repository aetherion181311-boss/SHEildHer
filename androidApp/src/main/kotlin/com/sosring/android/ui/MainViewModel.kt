package com.sosring.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sosring.ble.AndroidBleManager
import com.sosring.android.service.*
import com.sosring.contacts.EmergencyContact
import com.sosring.sos.SosEngine
import com.sosring.sos.AppUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _appState = MutableStateFlow(AppUiState())
    val appState: StateFlow<AppUiState> = _appState

    private val contactRepo = InMemoryContactRepository()
    val contacts: StateFlow<List<EmergencyContact>> = contactRepo.contactsFlow

    private var engine: SosEngine? = null

    init {
        viewModelScope.launch {
            try {
                val bleManager       = AndroidBleManager(app)
                val locationProvider = AndroidLocationProvider(app)
                val smsService       = AndroidSmsService(app)

                engine = SosEngine(
                    bleManager, locationProvider, contactRepo, smsService, viewModelScope
                ).also {
                    it.init()
                    it.appState.collect { state -> _appState.value = state }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerSos() = viewModelScope.launch {
        try { engine?.handleTrigger() } catch (e: Exception) { e.printStackTrace() }
    }

    fun cancelSos() = viewModelScope.launch {
        try { engine?.handleCancel() } catch (e: Exception) { e.printStackTrace() }
    }

    fun addContact(name: String, phone: String) = viewModelScope.launch {
        contactRepo.addContact(EmergencyContact(name = name, phoneNumber = phone))
    }

    fun removeContact(id: Long) = viewModelScope.launch {
        contactRepo.removeContact(id)
    }

    override fun onCleared() {
        try { engine?.cleanup() } catch (e: Exception) { e.printStackTrace() }
    }
}
