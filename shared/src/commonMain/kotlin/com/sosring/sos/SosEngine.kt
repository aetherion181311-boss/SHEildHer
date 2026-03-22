package com.sosring.sos

import com.sosring.ble.*
import com.sosring.contacts.ContactRepository
import com.sosring.contacts.EmergencyContact
import com.sosring.contacts.POLICE_NUMBER
import com.sosring.location.LocationProvider
import com.sosring.location.LocationState
import com.sosring.sms.SmsService
import com.sosring.sms.SmsTemplates
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ─── SOS state model ──────────────────────────────────────────────────────────
sealed class SosState {
    object Idle : SosState()
    data class Countdown(val secondsRemaining: Int) : SosState()   // 20-s countdown
    data class Active(val startedAt: Long) : SosState()            // alert sent, 2-min window
    object Cancelled : SosState()
    object Completed : SosState()
}

data class AppUiState(
    val bleState: BleConnectionState = BleConnectionState.Disconnected,
    val sosState: SosState = SosState.Idle,
    val isGpsEnabled: Boolean = false,
    val isSetupComplete: Boolean = false,
    val lastSmsSuccess: Boolean? = null
)

// ─── SOS Engine ──────────────────────────────────────────────────────────────
/**
 * Platform-agnostic SOS orchestration layer.
 * Drives the full workflow: BLE → countdown → SMS → 2-min escalation.
 */
class SosEngine(
    private val bleManager: BleManager,
    private val locationProvider: LocationProvider,
    private val contactRepo: ContactRepository,
    private val smsService: SmsService,
    private val scope: CoroutineScope
) {
    private val _appState = MutableStateFlow(AppUiState())
    val appState: StateFlow<AppUiState> = _appState.asStateFlow()

    private var countdownJob: Job? = null
    private var escalationJob: Job? = null

    companion object {
        const val COUNTDOWN_SECONDS = 20
        const val ESCALATION_WINDOW_MS = 120_000L  // 2 minutes
    }

    fun init() {
        observeBleState()
        observeTouchPatterns()
        observeGps()
        startBle()
    }

    // ── BLE bootstrap ────────────────────────────────────────────────────────
    private fun startBle() {
        scope.launch {
            runCatching { bleManager.startScanAndConnect() }
        }
    }

    private fun observeBleState() {
        scope.launch {
            bleManager.connectionState.collect { state ->
                _appState.update { it.copy(bleState = state) }
                if (state is BleConnectionState.Disconnected) {
                    // Auto-reconnect after 3 s
                    delay(3_000)
                    runCatching { bleManager.startScanAndConnect() }
                }
            }
        }
    }

    private fun observeTouchPatterns() {
        scope.launch {
            bleManager.touchPatterns.collect { pattern ->
                when (pattern) {
                    is TouchPattern.Trigger -> {
                        bleManager.sendAcknowledgement(BleConstants.ACK_TRIGGER)
                        handleTrigger()
                    }
                    is TouchPattern.Cancel -> {
                        bleManager.sendAcknowledgement(BleConstants.ACK_CANCEL)
                        handleCancel()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun observeGps() {
        scope.launch {
            locationProvider.locationState.collect { state ->
                _appState.update { it.copy(isGpsEnabled = state !is com.sosring.location.LocationState.GpsDisabled) }
            }
        }
    }

    // ── Trigger flow ─────────────────────────────────────────────────────────
    /** Starts SOS – callable from ring trigger OR manual button. */
    fun handleTrigger() {
        if (_appState.value.sosState !is SosState.Idle) return
        startCountdown()
    }

    fun handleCancel() {
        val state = _appState.value.sosState
        if (state is SosState.Idle || state is SosState.Completed) return

        countdownJob?.cancel()
        escalationJob?.cancel()
        _appState.update { it.copy(sosState = SosState.Cancelled) }

        // Reset to idle after brief display
        scope.launch {
            delay(3_000)
            _appState.update { it.copy(sosState = SosState.Idle) }
        }
    }

    private fun startCountdown() {
        _appState.update { it.copy(sosState = SosState.Countdown(COUNTDOWN_SECONDS)) }
        countdownJob = scope.launch {
            for (sec in COUNTDOWN_SECONDS downTo 1) {
                _appState.update { it.copy(sosState = SosState.Countdown(sec)) }
                delay(1_000)
            }
            sendAlerts()
        }
    }

    private suspend fun sendAlerts() {
        val contacts = contactRepo.getEmergencyContacts()
        val locationText = resolveLocationText()
        val message = SmsTemplates.sosAlert(locationText)

        // Send to all personal contacts
        contacts.filter { !it.isEmergencyService }.forEach { contact ->
            smsService.sendSms(contact.phoneNumber, message)
        }

        val now = currentTimeMillis()
        _appState.update { it.copy(sosState = SosState.Active(now)) }

        // 2-minute escalation to emergency services
        escalationJob = scope.launch {
            delay(ESCALATION_WINDOW_MS)
            if (_appState.value.sosState is SosState.Active) {
                val policeMsg = SmsTemplates.sosAlertPolice(locationText)
                smsService.sendSms(POLICE_NUMBER, policeMsg)
                _appState.update { it.copy(sosState = SosState.Completed) }
                delay(5_000)
                _appState.update { it.copy(sosState = SosState.Idle) }
            }
        }
    }

    private suspend fun resolveLocationText(): String {
        return when (val state = locationProvider.locationState.firstOrNull()) {
            is com.sosring.location.LocationState.Available ->
                state.data.toShareText()
            else -> {
                val last = locationProvider.getLastKnownLocation()
                last?.copy(isLive = false)?.toShareText()
                    ?: "Location unavailable"
            }
        }
    }

    fun cleanup() {
        countdownJob?.cancel()
        escalationJob?.cancel()
        bleManager.disconnect()
    }
}

// Expect/actual for time
expect fun currentTimeMillis(): Long
