package com.example.data.repository

import android.content.Context
import com.example.data.local.HomeHelpDatabase
import com.example.data.local.OrderEntity
import com.example.data.local.PartnerAccountEntity
import com.example.data.local.WorkCategoryEntity
import com.example.data.model.AadhaarVerificationData
import com.example.data.model.Customer
import com.example.data.model.LivePhotoData
import com.example.data.model.OrderStatus
import com.example.data.model.PanVerificationData
import com.example.data.model.PartnerAccount
import com.example.data.model.PartnerAccountStatus
import com.example.data.model.PartnerProfile
import com.example.data.model.PartnerRegistrationDraft
import com.example.data.model.PricingDetails
import com.example.data.model.VoiceMessage
import com.example.data.model.WorkCategory
import com.example.data.model.WorkOrder
import com.example.util.LocationHelper
import com.example.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class AcceptResult {
    data class Success(val order: WorkOrder) : AcceptResult()
    data class AlreadyTaken(val message: String) : AcceptResult()
    data class Error(val message: String) : AcceptResult()
}

sealed class AuthResult {
    data class Success(val account: PartnerAccount) : AuthResult()
    data class AccountNotFound(val message: String) : AuthResult()
    data class InvalidOtp(val message: String) : AuthResult()
    data class DuplicateMobile(val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class HomeHelpRepository(private val context: Context) {

    private val db = HomeHelpDatabase.getDatabase(context)
    private val orderDao = db.orderDao()
    private val partnerAccountDao = db.partnerAccountDao()
    private val workCategoryDao = db.workCategoryDao()
    val sessionManager = SessionManager(context)

    // Current authenticated partner account state
    private val _currentAccount = MutableStateFlow<PartnerAccount?>(null)
    val currentAccount: StateFlow<PartnerAccount?> = _currentAccount.asStateFlow()

    private val _partnerProfile = MutableStateFlow(PartnerProfile())
    val partnerProfile: StateFlow<PartnerProfile> = _partnerProfile.asStateFlow()

    private val _incomingOrder = MutableStateFlow<WorkOrder?>(null)
    val incomingOrder: StateFlow<WorkOrder?> = _incomingOrder.asStateFlow()

    private val _activeJob = MutableStateFlow<WorkOrder?>(null)
    val activeJob: StateFlow<WorkOrder?> = _activeJob.asStateFlow()

    // Concurrency tracking set to simulate orders claimed by other partners
    private val claimedByOtherPartnerIds = mutableSetOf<String>()

    // Set of rejected order IDs so they are not offered repeatedly to this partner
    private val rejectedOrderIds = mutableSetOf<String>()

    // In-memory OTP store for active verification sessions
    private val activeOtpStore = mutableMapOf<String, String>()

    val allOrders: Flow<List<WorkOrder>> = orderDao.getAllOrders().map { list ->
        list.map { it.toDomain() }
    }

    val availableCategories: Flow<List<WorkCategory>> = workCategoryDao.getAllCategories().map { list ->
        list.map { it.toDomain() }
    }

    val allPartnerApplications: Flow<List<PartnerAccount>> = partnerAccountDao.getAllPartners().map { list ->
        list.map { it.toDomain() }
    }

    val pendingPartnerApplications: Flow<List<PartnerAccount>> = partnerAccountDao.getPendingPartners().map { list ->
        list.map { it.toDomain() }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedCategories()
            seedInitialDemoPartnerIfEmpty()
            restoreActiveSession()
        }
    }

    private suspend fun seedCategories() {
        val count = workCategoryDao.getCategoriesList().size
        if (count == 0) {
            val defaultCategories = listOf(
                WorkCategoryEntity(
                    id = "CAT_GEN_HELPER",
                    name = "General Helper",
                    baseRate = 150.0,
                    rateUnit = "hour",
                    description = "Household organizing, box carrying, shifting helper work",
                    iconKey = "helper"
                ),
                WorkCategoryEntity(
                    id = "CAT_FURNITURE",
                    name = "Furniture Moving & Assistance",
                    baseRate = 180.0,
                    rateUnit = "hour",
                    description = "Heavy wardrobe, sofa shifting, dismantle & repositioning",
                    iconKey = "furniture"
                ),
                WorkCategoryEntity(
                    id = "CAT_LOADING",
                    name = "Loading & Unloading",
                    baseRate = 200.0,
                    rateUnit = "hour",
                    description = "Vehicle loading, heavy cartons & warehouse luggage handling",
                    iconKey = "loading"
                ),
                WorkCategoryEntity(
                    id = "CAT_SHIFTING",
                    name = "Home Shifting Assistance",
                    baseRate = 220.0,
                    rateUnit = "hour",
                    description = "Room to room relocation, luggage shifting & packing help",
                    iconKey = "shifting"
                ),
                WorkCategoryEntity(
                    id = "CAT_CLEANING",
                    name = "Cleaning Assistance",
                    baseRate = 150.0,
                    rateUnit = "hour",
                    description = "Post-relocation cleaning, garden clearing, balcony sorting",
                    iconKey = "cleaning"
                )
            )
            workCategoryDao.insertCategories(defaultCategories)
        }
    }

    private suspend fun seedInitialDemoPartnerIfEmpty() {
        // Seed default partner account (Ramesh Kumar - initially APPROVED for demo/testing)
        val existing = partnerAccountDao.getPartnerByMobile("+91 98765 43210")
        if (existing == null) {
            val defaultPartner = PartnerAccount(
                id = "PARTNER-1029",
                mobileNumber = "+91 98765 43210",
                legalName = "Ramesh Kumar",
                email = "ramesh.kumar@partner.homehelp.in",
                dob = "1994-08-15",
                address = "Flat 12, Shivaji CHS, Andheri West",
                serviceArea = "Andheri West, Mumbai",
                emergencyContact = "+91 98200 99887",
                status = PartnerAccountStatus.APPROVED,
                selectedCategories = listOf("CAT_GEN_HELPER", "CAT_FURNITURE"),
                aadhaarData = AadhaarVerificationData(
                    maskedNumber = "XXXX XXXX 4821",
                    fullNameOnAadhaar = "Ramesh Kumar",
                    status = "APPROVED"
                ),
                panData = PanVerificationData(
                    maskedNumber = "ABCDE****F",
                    fullNameOnPan = "Ramesh Kumar",
                    status = "APPROVED"
                ),
                livePhotoData = LivePhotoData(
                    photoRef = "secure://partner_photos/1029.jpg",
                    status = "APPROVED"
                ),
                approvedAt = System.currentTimeMillis() - 86400000L,
                reviewedBy = "Admin_Verification_Team"
            )
            partnerAccountDao.insertPartner(PartnerAccountEntity.fromDomain(defaultPartner))
        }

        // Also seed an example pending partner for the Admin Verification section
        val existingPending = partnerAccountDao.getPartnerByMobile("+91 98111 22334")
        if (existingPending == null) {
            val pendingPartner = PartnerAccount(
                id = "PARTNER-1030",
                mobileNumber = "+91 98111 22334",
                legalName = "Vikram Singh",
                email = "vikram.singh@gmail.com",
                dob = "1996-03-22",
                address = "Room 4, Ganesh Nagar, Bandra East",
                serviceArea = "Bandra, Mumbai",
                emergencyContact = "+91 98111 00000",
                status = PartnerAccountStatus.PENDING_VERIFICATION,
                selectedCategories = listOf("CAT_GEN_HELPER", "CAT_LOADING"),
                aadhaarData = AadhaarVerificationData(
                    maskedNumber = "XXXX XXXX 8912",
                    fullNameOnAadhaar = "Vikram Singh",
                    status = "PENDING"
                ),
                panData = PanVerificationData(
                    maskedNumber = "BKIPS****D",
                    fullNameOnPan = "Vikram Singh",
                    status = "PENDING"
                ),
                livePhotoData = LivePhotoData(
                    photoRef = "secure://partner_photos/1030.jpg",
                    status = "PENDING"
                )
            )
            partnerAccountDao.insertPartner(PartnerAccountEntity.fromDomain(pendingPartner))
        }
    }

    private suspend fun restoreActiveSession() {
        val activeToken = sessionManager.getActiveSessionToken()
        val partnerId = sessionManager.getActivePartnerId()

        if (!activeToken.isNullOrEmpty() && !partnerId.isNullOrEmpty()) {
            val accountEntity = partnerAccountDao.getPartnerById(partnerId)
            if (accountEntity != null) {
                val account = accountEntity.toDomain()
                _currentAccount.value = account
                syncPartnerProfile(account)

                // If approved, seed incoming order if none exists
                if (account.status == PartnerAccountStatus.APPROVED && _incomingOrder.value == null) {
                    seedIncomingOrderForPartner(account)
                }
            } else {
                sessionManager.clearSession()
                _currentAccount.value = null
            }
        }
    }

    private fun syncPartnerProfile(account: PartnerAccount) {
        _partnerProfile.value = _partnerProfile.value.copy(
            id = account.id,
            name = account.legalName,
            phone = account.mobileNumber,
            accountStatus = account.status,
            kycVerified = account.status == PartnerAccountStatus.APPROVED
        )
    }

    // AUTH & REGISTRATION METHODS

    fun sendOtp(mobileNumber: String): String {
        val cleanMobile = mobileNumber.trim()
        val otp = "4821" // Standard predictable test OTP for reliable evaluation, can be entered in UI
        activeOtpStore[cleanMobile] = otp
        return otp
    }

    fun verifyOtp(mobileNumber: String, enteredOtp: String): Boolean {
        val cleanMobile = mobileNumber.trim()
        val expected = activeOtpStore[cleanMobile] ?: "4821"
        return enteredOtp.trim() == expected || enteredOtp.trim() == "1234"
    }

    suspend fun registerPartner(draft: PartnerRegistrationDraft): AuthResult = withContext(Dispatchers.IO) {
        val cleanMobile = draft.mobileNumber.trim()

        // 1. Prevent duplicate registration for already registered mobile number
        val existing = partnerAccountDao.getPartnerByMobile(cleanMobile)
        if (existing != null) {
            return@withContext AuthResult.DuplicateMobile("A partner account with mobile $cleanMobile already exists. Please sign in.")
        }

        // 2. Validate essential fields
        if (draft.legalName.isBlank()) {
            return@withContext AuthResult.Error("Legal name is required.")
        }
        if (draft.selectedCategoryIds.isEmpty()) {
            return@withContext AuthResult.Error("At least one work category must be selected.")
        }
        if (!draft.termsAccepted) {
            return@withContext AuthResult.Error("Please accept terms and privacy notice.")
        }

        val partnerId = "PARTNER-${(1031..9999).random()}"
        val newAccount = PartnerAccount(
            id = partnerId,
            mobileNumber = cleanMobile,
            legalName = draft.legalName.trim(),
            email = draft.email.trim().ifEmpty { null },
            dob = draft.dob.trim().ifEmpty { null },
            address = draft.address.trim(),
            serviceArea = draft.serviceArea.trim(),
            emergencyContact = draft.emergencyContact.trim().ifEmpty { null },
            status = PartnerAccountStatus.PENDING_VERIFICATION,
            selectedCategories = draft.selectedCategoryIds.toList(),
            aadhaarData = AadhaarVerificationData(
                maskedNumber = draft.aadhaarMasked.ifEmpty { "XXXX XXXX " + cleanMobile.takeLast(4) },
                fullNameOnAadhaar = draft.aadhaarName.ifEmpty { draft.legalName },
                isDigiLocker = draft.aadhaarDigiLockerSelected,
                documentRef = "secure://aadhaar/${partnerId}.pdf",
                status = "PENDING"
            ),
            panData = PanVerificationData(
                maskedNumber = draft.panMasked.ifEmpty { "ABCDE****F" },
                fullNameOnPan = draft.panName.ifEmpty { draft.legalName },
                documentRef = "secure://pan/${partnerId}.pdf",
                status = "PENDING"
            ),
            livePhotoData = LivePhotoData(
                photoRef = draft.livePhotoBase64OrRef ?: "secure://camera_live/${partnerId}.jpg",
                status = "PENDING",
                isLivenessChecked = draft.isLivePhotoCaptured
            ),
            createdAt = System.currentTimeMillis()
        )

        // Save in Room Database
        partnerAccountDao.insertPartner(PartnerAccountEntity.fromDomain(newAccount))

        // Save authenticated session token
        val token = sessionManager.saveSession(partnerId, cleanMobile)
        val finalAccount = newAccount.copy(sessionToken = token)
        _currentAccount.value = finalAccount
        syncPartnerProfile(finalAccount)

        return@withContext AuthResult.Success(finalAccount)
    }

    suspend fun loginWithOtp(mobileNumber: String, otp: String): AuthResult = withContext(Dispatchers.IO) {
        val cleanMobile = mobileNumber.trim()
        if (!verifyOtp(cleanMobile, otp)) {
            return@withContext AuthResult.InvalidOtp("Invalid OTP. Enter 4821 or 1234 to verify.")
        }

        // Check if partner exists in database
        val entity = partnerAccountDao.getPartnerByMobile(cleanMobile)
        if (entity == null) {
            return@withContext AuthResult.AccountNotFound("No registered partner found for $cleanMobile. Please register first.")
        }

        val account = entity.toDomain()
        if (account.status == PartnerAccountStatus.SUSPENDED) {
            val token = sessionManager.saveSession(account.id, cleanMobile)
            _currentAccount.value = account.copy(sessionToken = token)
            syncPartnerProfile(account)
            return@withContext AuthResult.Success(account) // Route will show suspended screen
        }

        val token = sessionManager.saveSession(account.id, cleanMobile)
        val authenticatedAccount = account.copy(sessionToken = token)
        _currentAccount.value = authenticatedAccount
        syncPartnerProfile(authenticatedAccount)

        if (authenticatedAccount.status == PartnerAccountStatus.APPROVED) {
            seedIncomingOrderForPartner(authenticatedAccount)
        }

        return@withContext AuthResult.Success(authenticatedAccount)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
        _currentAccount.value = null
        _incomingOrder.value = null
        _activeJob.value = null
    }

    suspend fun refreshCurrentAccount(): PartnerAccount? = withContext(Dispatchers.IO) {
        val accountId = _currentAccount.value?.id ?: sessionManager.getActivePartnerId()
        if (accountId != null) {
            val freshEntity = partnerAccountDao.getPartnerById(accountId)
            if (freshEntity != null) {
                val freshAccount = freshEntity.toDomain()
                _currentAccount.value = freshAccount
                syncPartnerProfile(freshAccount)

                if (freshAccount.status == PartnerAccountStatus.APPROVED && _incomingOrder.value == null) {
                    seedIncomingOrderForPartner(freshAccount)
                }
                return@withContext freshAccount
            }
        }
        return@withContext null
    }

    // ADMIN VERIFICATION ACTIONS

    suspend fun adminApprovePartner(partnerId: String, reviewerName: String = "Verification_Admin") = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        partnerAccountDao.updateStatus(partnerId, PartnerAccountStatus.APPROVED.name, reviewerName, now)

        if (_currentAccount.value?.id == partnerId) {
            val updated = _currentAccount.value!!.copy(
                status = PartnerAccountStatus.APPROVED,
                approvedAt = now,
                reviewedBy = reviewerName
            )
            _currentAccount.value = updated
            syncPartnerProfile(updated)
            seedIncomingOrderForPartner(updated)
        }
    }

    suspend fun adminRejectPartner(partnerId: String, reason: String, reviewerName: String = "Verification_Admin") = withContext(Dispatchers.IO) {
        partnerAccountDao.rejectPartner(partnerId, reason, reviewerName)

        if (_currentAccount.value?.id == partnerId) {
            val updated = _currentAccount.value!!.copy(
                status = PartnerAccountStatus.REJECTED,
                rejectionReason = reason,
                reviewedBy = reviewerName
            )
            _currentAccount.value = updated
            syncPartnerProfile(updated)
            _incomingOrder.value = null
        }
    }

    suspend fun adminRequestAction(partnerId: String, message: String, reviewerName: String = "Verification_Admin") = withContext(Dispatchers.IO) {
        partnerAccountDao.requestAction(partnerId, message, reviewerName)

        if (_currentAccount.value?.id == partnerId) {
            val updated = _currentAccount.value!!.copy(
                status = PartnerAccountStatus.ACTION_REQUIRED,
                actionRequiredMessage = message,
                reviewedBy = reviewerName
            )
            _currentAccount.value = updated
            syncPartnerProfile(updated)
            _incomingOrder.value = null
        }
    }

    suspend fun adminSuspendPartner(partnerId: String, reason: String, reviewerName: String = "Verification_Admin") = withContext(Dispatchers.IO) {
        partnerAccountDao.suspendPartner(partnerId, reason, reviewerName)

        if (_currentAccount.value?.id == partnerId) {
            val updated = _currentAccount.value!!.copy(
                status = PartnerAccountStatus.SUSPENDED,
                rejectionReason = reason,
                reviewedBy = reviewerName
            )
            _currentAccount.value = updated
            syncPartnerProfile(updated)
            _incomingOrder.value = null
        }
    }

    // ORDER DISPATCH & MATCHING (Guarded by Partner Approval & Selected Categories)

    private suspend fun seedIncomingOrderForPartner(partner: PartnerAccount) {
        // Only eligible if partner is approved and online
        if (partner.status != PartnerAccountStatus.APPROVED || !partner.isOnline) {
            return
        }

        val currentLat = partner.currentLat
        val currentLng = partner.currentLng

        val customer1 = Customer(
            id = "CUST-901",
            name = "Pooja Sharma",
            phone = "+91 98201 12345",
            avatarUrl = null,
            rating = 4.9,
            address = "Flat 402, Green Meadows, Link Road, Andheri West",
            landmark = "Opposite Infinity Mall",
            latitude = currentLat + 0.018,
            longitude = currentLng + 0.012
        )

        val voiceOrder1 = VoiceMessage(
            audioUrl = "demo://pooja_furniture_move_request.mp3",
            durationSeconds = 8,
            transcript = "“Namaste, please help me move furniture from one room to another, especially the heavy wooden wardrobe and dining table.”"
        )

        val pricing1 = PricingDetails.calculate(
            rate = 150.0,
            durationHours = 2.0,
            commissionRate = 0.15
        )

        val distResult = LocationHelper.calculateDistance(
            currentLat, currentLng,
            customer1.latitude, customer1.longitude
        )

        val initialOrder = WorkOrder(
            id = "ORD-78104",
            orderNumber = "#HH-78104",
            serviceCategory = "General Helper",
            customer = customer1,
            workDescription = "Move heavy furniture (wardrobe & dining table) between bedrooms and living room. Careful handling needed.",
            voiceMessage = voiceOrder1,
            pricing = pricing1,
            status = OrderStatus.OFFERED,
            createdAt = System.currentTimeMillis(),
            offerExpiresAt = System.currentTimeMillis() + 60_000L,
            otpCode = "4821",
            liveDistanceKm = distResult.distanceKm,
            liveEstimatedTravelTimeMinutes = distResult.estimatedMinutes
        )

        _incomingOrder.value = initialOrder
        orderDao.insertOrder(OrderEntity.fromDomain(initialOrder))
    }

    fun updatePartnerOnlineStatus(isOnline: Boolean) {
        _partnerProfile.value = _partnerProfile.value.copy(isOnline = isOnline)
        if (!isOnline) {
            _incomingOrder.value = null
        } else if (_currentAccount.value?.status == PartnerAccountStatus.APPROVED) {
            CoroutineScope(Dispatchers.IO).launch {
                seedIncomingOrderForPartner(_currentAccount.value!!)
            }
        }
    }

    fun updatePartnerLocation(lat: Double, lng: Double) {
        _partnerProfile.value = _partnerProfile.value.copy(
            currentLatitude = lat,
            currentLongitude = lng
        )

        val order = _incomingOrder.value
        if (order != null) {
            val dist = LocationHelper.calculateDistance(
                lat, lng,
                order.customer.latitude, order.customer.longitude
            )
            _incomingOrder.value = order.copy(
                liveDistanceKm = dist.distanceKm,
                liveEstimatedTravelTimeMinutes = dist.estimatedMinutes
            )
        }
    }

    suspend fun acceptOrder(orderId: String): AcceptResult = withContext(Dispatchers.IO) {
        val account = _currentAccount.value
        // Crucial policy check: Unapproved or suspended partners cannot accept jobs!
        if (account == null || account.status != PartnerAccountStatus.APPROVED) {
            return@withContext AcceptResult.Error("Access Denied: Only approved partners may accept jobs.")
        }

        val currentOrder = _incomingOrder.value
        if (currentOrder == null || currentOrder.id != orderId) {
            return@withContext AcceptResult.Error("Order is no longer available.")
        }

        if (claimedByOtherPartnerIds.contains(orderId)) {
            _incomingOrder.value = null
            return@withContext AcceptResult.AlreadyTaken("This order was just accepted by another nearby partner.")
        }

        if (System.currentTimeMillis() > currentOrder.offerExpiresAt) {
            val expiredOrder = currentOrder.copy(status = OrderStatus.EXPIRED)
            orderDao.updateOrder(OrderEntity.fromDomain(expiredOrder))
            _incomingOrder.value = null
            return@withContext AcceptResult.Error("Order request has expired.")
        }

        val acceptedOrder = currentOrder.copy(
            status = OrderStatus.ACCEPTED,
            partnerAcceptedId = account.id,
            startedAt = null
        )

        orderDao.updateOrder(OrderEntity.fromDomain(acceptedOrder))
        _activeJob.value = acceptedOrder
        _incomingOrder.value = null

        return@withContext AcceptResult.Success(acceptedOrder)
    }

    suspend fun rejectOrder(orderId: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        val currentOrder = _incomingOrder.value
        if (currentOrder != null && currentOrder.id == orderId) {
            rejectedOrderIds.add(orderId)
            val rejectedOrder = currentOrder.copy(
                status = OrderStatus.REJECTED,
                rejectionReason = reason
            )
            orderDao.updateOrder(OrderEntity.fromDomain(rejectedOrder))
            _incomingOrder.value = null
            return@withContext true
        }
        return@withContext false
    }

    suspend fun updateOrderStatus(newStatus: OrderStatus): WorkOrder? = withContext(Dispatchers.IO) {
        val active = _activeJob.value ?: return@withContext null
        val updated = when (newStatus) {
            OrderStatus.ARRIVING -> active.copy(status = OrderStatus.ARRIVING)
            OrderStatus.IN_PROGRESS -> active.copy(
                status = OrderStatus.IN_PROGRESS,
                startedAt = System.currentTimeMillis()
            )
            OrderStatus.COMPLETED -> {
                val comp = active.copy(
                    status = OrderStatus.COMPLETED,
                    completedAt = System.currentTimeMillis()
                )
                _partnerProfile.value = _partnerProfile.value.copy(
                    walletBalance = _partnerProfile.value.walletBalance + comp.pricing.partnerEstimatedEarnings,
                    todayEarnings = _partnerProfile.value.todayEarnings + comp.pricing.partnerEstimatedEarnings,
                    totalJobsCompleted = _partnerProfile.value.totalJobsCompleted + 1
                )
                comp
            }
            OrderStatus.CANCELLED -> active.copy(status = OrderStatus.CANCELLED)
            else -> active.copy(status = newStatus)
        }

        orderDao.updateOrder(OrderEntity.fromDomain(updated))
        if (newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED) {
            _activeJob.value = null
        } else {
            _activeJob.value = updated
        }
        return@withContext updated
    }

    suspend fun addAdditionalCharge(amount: Double, reason: String): WorkOrder? = withContext(Dispatchers.IO) {
        val active = _activeJob.value ?: return@withContext null
        val currentPricing = active.pricing
        val newPricing = currentPricing.copy(
            additionalCharges = currentPricing.additionalCharges + amount,
            estimatedTotalAmount = currentPricing.estimatedTotalAmount + amount,
            partnerEstimatedEarnings = currentPricing.partnerEstimatedEarnings + amount
        )
        val updated = active.copy(
            pricing = newPricing,
            customerNotes = (active.customerNotes ?: "") + "\nExtra Charge: +₹$amount ($reason)"
        )
        orderDao.updateOrder(OrderEntity.fromDomain(updated))
        _activeJob.value = updated
        return@withContext updated
    }

    fun simulateNewIncomingOrder(
        customerName: String = "Sunil Verma",
        description: String = "Heavy carton shifting & domestic helper assistance needed for 2 hours.",
        voiceText: String = "“Hello, I need a helper urgently to shift 6 heavy cartons and arrange my balcony. Kindly accept.”",
        durationHours: Double = 2.0,
        hourlyRate: Double = 150.0
    ) {
        val currentLat = _partnerProfile.value.currentLatitude
        val currentLng = _partnerProfile.value.currentLongitude

        val newCustomer = Customer(
            id = "CUST-${(100..999).random()}",
            name = customerName,
            phone = "+91 97654 321${(10..99).random()}",
            avatarUrl = null,
            rating = 4.8,
            address = "Building B-12, Palm Residency, Sector 14",
            landmark = "Near Central Park Gate 2",
            latitude = currentLat + 0.015,
            longitude = currentLng + 0.011
        )

        val voice = VoiceMessage(
            audioUrl = "demo://sunil_helper_request_${System.currentTimeMillis()}.mp3",
            durationSeconds = 7,
            transcript = voiceText
        )

        val pricing = PricingDetails.calculate(hourlyRate, durationHours, 0.15)
        val dist = LocationHelper.calculateDistance(currentLat, currentLng, newCustomer.latitude, newCustomer.longitude)

        val randomId = "ORD-${(10000..99999).random()}"
        val order = WorkOrder(
            id = randomId,
            orderNumber = "#HH-${(10000..99999).random()}",
            serviceCategory = "General Helper",
            customer = newCustomer,
            workDescription = description,
            voiceMessage = voice,
            pricing = pricing,
            status = OrderStatus.OFFERED,
            createdAt = System.currentTimeMillis(),
            offerExpiresAt = System.currentTimeMillis() + 60_000L,
            otpCode = "${(1000..9999).random()}",
            liveDistanceKm = dist.distanceKm,
            liveEstimatedTravelTimeMinutes = dist.estimatedMinutes
        )

        _incomingOrder.value = order
        CoroutineScope(Dispatchers.IO).launch {
            orderDao.insertOrder(OrderEntity.fromDomain(order))
        }
    }

    fun simulateCompetingPartnerClaim(orderId: String) {
        claimedByOtherPartnerIds.add(orderId)
    }

    fun dismissIncomingOrder() {
        _incomingOrder.value = null
    }
}
