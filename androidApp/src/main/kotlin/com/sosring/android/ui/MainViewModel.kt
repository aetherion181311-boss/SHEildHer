package com.sosring.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosring.ble.AndroidBleManager
import com.sosring.android.service.*
import com.sosring.sos.SosEngine
import com.sosring.sos.AppUiState
import kotlinx.coroutines.flow.StateFlow
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val bleManager      = AndroidBleManager(app)
    private val locationProvider= AndroidLocationProvider(app)
    private val contactRepo     = InMemoryContactRepository()
    private val smsService      = AndroidSmsService(app)

    private val engine = SosEngine(
        bleManager, locationProvider, contactRepo, smsService, viewModelScope
    )

    val appState: StateFlow<AppUiState> = engine.appState

    init { engine.init() }

    fun triggerSos() = engine.handleTrigger()
    fun cancelSos()  = engine.handleCancel()

    override fun onCleared() { engine.cleanup() }
}
