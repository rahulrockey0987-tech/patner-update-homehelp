package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.OrderStatus
import com.example.data.model.PartnerAccount
import com.example.data.model.PartnerAccountStatus
import com.example.data.model.PartnerProfile
import com.example.data.model.PartnerRegistrationDraft
import com.example.data.model.WorkCategory
import com.example.data.model.WorkOrder
import com.example.data.repository.AcceptResult
import com.example.data.repository.AuthResult
import com.example.data.repository.HomeHelpRepository
import com.example.util.LocationHelper
import com.example.util.OrderAlarmManager
import com.example.util.VoiceOrderPlayer
import com.example.util.VoicePlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class OrderAcceptedSuccess(val orderNumber: String) : UiEvent()
    data class OrderAlreadyTakenAlert(val message: String) : UiEvent()
    data class RegistrationSubmitted(val partnerId: String) : UiEvent()
    data class LoginSuccess(val account: PartnerAccount) : UiEvent()
    data class AuthError(val error: String) : UiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = HomeHelpRepository(application)
    val alarmManager = OrderAlarmManager(application)
    val voicePlayer = VoiceOrderPlayer(application)

    val currentAccount: StateFlow<PartnerAccount?> = repository.currentAccount
    val partnerProfile: StateFlow<PartnerProfile> = repository.partnerProfile
    val incomingOrder: StateFlow<WorkOrder?> = repository.incomingOrder
    val activeJob: StateFlow<WorkOrder?> = repository.activeJob
    val allOrders: StateFlow<List<WorkOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCategories: StateFlow<List<WorkCategory>> = repository.availableCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingPartnerApplications: StateFlow<List<PartnerAccount>> = repository.pendingPartnerApplications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voicePlayerState: StateFlow<VoicePlayerState> = voicePlayer.state

    // Registration wizard state
    private val _registrationDraft = MutableStateFlow(PartnerRegistrationDraft())
    val registrationDraft: StateFlow<PartnerRegistrationDraft> = _registrationDraft.asStateFlow()

    private val _registrationStep = MutableStateFlow(1) // 1 to 6
    val registrationStep: StateFlow<Int> = _registrationStep.asStateFlow()

    // Live countdown for incoming order acceptance
    private val _timeRemainingSeconds = MutableStateFlow(60)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _isAlarmSounding = MutableStateFlow(false)
    val isAlarmSounding: StateFlow<Boolean> = _isAlarmSounding.asStateFlow()

    private val _isProcessingAction = MutableStateFlow(false)
    val isProcessingAction: StateFlow<Boolean> = _isProcessingAction.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    private var countdownJob: Job? = null
    private var lastHandledOrderId: String? = null

    init {
        // Observe incoming orders: only sound alarm if current partner is APPROVED!
        viewModelScope.launch {
            incomingOrder.collect { order ->
                val account = currentAccount.value
                val isApproved = account == null || account.status == PartnerAccountStatus.APPROVED
                if (order != null && order.status == OrderStatus.OFFERED && isApproved) {
                    if (lastHandledOrderId != order.id) {
                        lastHandledOrderId = order.id
                        startIncomingAlert(order)
                    }
                } else {
                    stopIncomingAlert()
                }
            }
        }
    }

    // AUTH & REGISTRATION WIZARD METHODS

    fun updateRegistrationDraft(update: (PartnerRegistrationDraft) -> PartnerRegistrationDraft) {
        _registrationDraft.value = update(_registrationDraft.value)
    }

    fun setRegistrationStep(step: Int) {
        _registrationStep.value = step.coerceIn(1, 6)
    }

    fun nextRegistrationStep() {
        _registrationStep.value = (_registrationStep.value + 1).coerceAtMost(6)
    }

    fun previousRegistrationStep() {
        _registrationStep.value = (_registrationStep.value - 1).coerceAtLeast(1)
    }

    fun resetRegistration() {
        _registrationDraft.value = PartnerRegistrationDraft()
        _registrationStep.value = 1
    }

    fun sendRegistrationOtp(mobileNumber: String): String {
        return repository.sendOtp(mobileNumber)
    }

    fun verifyRegistrationOtp(mobileNumber: String, enteredOtp: String): Boolean {
        val success = repository.verifyOtp(mobileNumber, enteredOtp)
        if (success) {
            updateRegistrationDraft { it.copy(isMobileOtpVerified = true) }
        }
        return success
    }

    fun submitRegistration() {
        if (_isProcessingAction.value) return
        _isProcessingAction.value = true

        viewModelScope.launch {
            try {
                when (val result = repository.registerPartner(_registrationDraft.value)) {
                    is AuthResult.Success -> {
                        _uiEvents.emit(UiEvent.RegistrationSubmitted(result.account.id))
                        _uiEvents.emit(UiEvent.ShowToast("Application submitted successfully for verification!"))
                    }
                    is AuthResult.DuplicateMobile -> {
                        _uiEvents.emit(UiEvent.AuthError(result.message))
                    }
                    is AuthResult.Error -> {
                        _uiEvents.emit(UiEvent.AuthError(result.message))
                    }
                    else -> {
                        _uiEvents.emit(UiEvent.AuthError("Registration failed. Please check your details."))
                    }
                }
            } finally {
                _isProcessingAction.value = false
            }
        }
    }

    fun loginWithOtp(mobileNumber: String, otp: String) {
        if (_isProcessingAction.value) return
        _isProcessingAction.value = true

        viewModelScope.launch {
            try {
                when (val result = repository.loginWithOtp(mobileNumber, otp)) {
                    is AuthResult.Success -> {
                        _uiEvents.emit(UiEvent.LoginSuccess(result.account))
                        _uiEvents.emit(UiEvent.ShowToast("Welcome back, ${result.account.legalName}!"))
                    }
                    is AuthResult.AccountNotFound -> {
                        _uiEvents.emit(UiEvent.AuthError(result.message))
                    }
                    is AuthResult.InvalidOtp -> {
                        _uiEvents.emit(UiEvent.AuthError(result.message))
                    }
                    else -> {
                        _uiEvents.emit(UiEvent.AuthError("Login failed. Please check credentials."))
                    }
                }
            } finally {
                _isProcessingAction.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            stopIncomingAlert()
            repository.logout()
            resetRegistration()
            _uiEvents.emit(UiEvent.ShowToast("Logged out successfully."))
        }
    }

    fun refreshAccountStatus() {
        viewModelScope.launch {
            val refreshed = repository.refreshCurrentAccount()
            if (refreshed != null) {
                _uiEvents.emit(UiEvent.ShowToast("Status updated: ${refreshed.status.label}"))
            }
        }
    }

    // ADMIN ACTIONS

    fun adminApprovePartner(partnerId: String) {
        viewModelScope.launch {
            repository.adminApprovePartner(partnerId)
            _uiEvents.emit(UiEvent.ShowToast("Partner #$partnerId APPROVED!"))
        }
    }

    fun adminRejectPartner(partnerId: String, reason: String) {
        viewModelScope.launch {
            repository.adminRejectPartner(partnerId, reason)
            _uiEvents.emit(UiEvent.ShowToast("Partner #$partnerId Rejected."))
        }
    }

    fun adminRequestAction(partnerId: String, message: String) {
        viewModelScope.launch {
            repository.adminRequestAction(partnerId, message)
            _uiEvents.emit(UiEvent.ShowToast("Correction request sent to partner."))
        }
    }

    fun adminSuspendPartner(partnerId: String, reason: String) {
        viewModelScope.launch {
            repository.adminSuspendPartner(partnerId, reason)
            _uiEvents.emit(UiEvent.ShowToast("Partner #$partnerId SUSPENDED."))
        }
    }

    // ORDERS & ALERTS

    private fun startIncomingAlert(order: WorkOrder) {
        val remainingMs = (order.offerExpiresAt - System.currentTimeMillis()).coerceAtLeast(0)
        _timeRemainingSeconds.value = (remainingMs / 1000).toInt()

        alarmManager.startAlarm(
            orderId = order.id,
            customerName = order.customer.name,
            rateText = "₹${order.pricing.hourlyRate.toInt()}/hr"
        )
        _isAlarmSounding.value = true

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive && _timeRemainingSeconds.value > 0) {
                delay(1000)
                _timeRemainingSeconds.value -= 1
            }
            if (_timeRemainingSeconds.value <= 0) {
                stopIncomingAlert()
                repository.dismissIncomingOrder()
                _uiEvents.emit(UiEvent.ShowToast("Order request expired."))
            }
        }
    }

    fun stopAlarmSound() {
        alarmManager.stopAlarm()
        _isAlarmSounding.value = false
    }

    private fun stopIncomingAlert() {
        stopAlarmSound()
        countdownJob?.cancel()
        countdownJob = null
        voicePlayer.stop()
    }

    fun toggleVoicePlayback(url: String, durationSec: Int) {
        voicePlayer.play(url, durationSec)
    }

    fun seekVoice(ratio: Float) {
        voicePlayer.seekTo(ratio)
    }

    fun acceptOrder(orderId: String) {
        if (_isProcessingAction.value) return
        _isProcessingAction.value = true
        stopIncomingAlert()

        viewModelScope.launch {
            try {
                when (val result = repository.acceptOrder(orderId)) {
                    is AcceptResult.Success -> {
                        _uiEvents.emit(UiEvent.OrderAcceptedSuccess(result.order.orderNumber))
                    }
                    is AcceptResult.AlreadyTaken -> {
                        _uiEvents.emit(UiEvent.OrderAlreadyTakenAlert(result.message))
                    }
                    is AcceptResult.Error -> {
                        _uiEvents.emit(UiEvent.ShowToast(result.message))
                    }
                }
            } finally {
                _isProcessingAction.value = false
            }
        }
    }

    fun rejectOrder(orderId: String, reason: String) {
        if (_isProcessingAction.value) return
        _isProcessingAction.value = true
        stopIncomingAlert()

        viewModelScope.launch {
            try {
                repository.rejectOrder(orderId, reason)
                _uiEvents.emit(UiEvent.ShowToast("Order passed to next eligible partner."))
            } finally {
                _isProcessingAction.value = false
            }
        }
    }

    fun updateOrderStatus(status: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(status)
        }
    }

    fun addAdditionalCharge(amount: Double, reason: String) {
        viewModelScope.launch {
            repository.addAdditionalCharge(amount, reason)
            _uiEvents.emit(UiEvent.ShowToast("Added extra charge of ₹$amount"))
        }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _hasLocationPermission.value = granted
        if (granted) {
            refreshPartnerLocation()
        }
    }

    fun refreshPartnerLocation() {
        LocationHelper.fetchCurrentLocation(
            context = getApplication(),
            onLocationReceived = { lat, lng ->
                repository.updatePartnerLocation(lat, lng)
            },
            onError = {
                // Keep default city location coordinates
            }
        )
    }

    fun toggleOnlineStatus(isOnline: Boolean) {
        repository.updatePartnerOnlineStatus(isOnline)
        if (!isOnline) {
            stopIncomingAlert()
        }
    }

    fun simulateNewOrder(customerName: String, description: String, voiceText: String, durationHours: Double) {
        repository.simulateNewIncomingOrder(
            customerName = customerName,
            description = description,
            voiceText = voiceText,
            durationHours = durationHours
        )
    }

    fun simulateCompetingClaim(orderId: String) {
        repository.simulateCompetingPartnerClaim(orderId)
    }

    override fun onCleared() {
        super.onCleared()
        stopIncomingAlert()
        voicePlayer.release()
    }
}
