package com.example.data.model

enum class OrderStatus {
    PENDING,
    OFFERED,       // Active live alert to partner
    ACCEPTED,      // Partner accepted
    ARRIVING,      // En route to customer
    IN_PROGRESS,   // Work started
    COMPLETED,     // Finished and billed
    REJECTED,      // Declined by partner
    EXPIRED,       // Acceptance countdown expired
    CANCELLED      // Cancelled
}

enum class PartnerAccountStatus(val label: String) {
    DRAFT("Draft"),
    PENDING_VERIFICATION("Pending Verification"),
    ACTION_REQUIRED("Action Required"),
    REJECTED("Rejected"),
    APPROVED("Approved"),
    SUSPENDED("Suspended")
}

data class Customer(
    val id: String,
    val name: String,
    val phone: String,
    val avatarUrl: String? = null,
    val rating: Double = 4.8,
    val address: String,
    val landmark: String? = null,
    val latitude: Double,
    val longitude: Double
)

data class VoiceMessage(
    val audioUrl: String,
    val durationSeconds: Int,
    val transcript: String? = null,
    val waveformPoints: List<Float> = listOf(0.3f, 0.6f, 0.9f, 0.4f, 0.8f, 1.0f, 0.7f, 0.5f, 0.9f, 0.6f, 0.4f, 0.8f, 0.5f)
)

data class PricingDetails(
    val hourlyRate: Double = 150.0,            // ₹150 per hour for General Helper
    val requestedDurationHours: Double? = 2.0, // null if open-ended
    val estimatedTotalAmount: Double = 300.0,
    val platformCommissionRate: Double = 0.15, // 15% backend commission
    val platformCommissionAmount: Double = 45.0,
    val partnerEstimatedEarnings: Double = 255.0,
    val isEstimated: Boolean = false,
    val additionalCharges: Double = 0.0
) {
    companion object {
        fun calculate(rate: Double = 150.0, durationHours: Double? = 1.0, commissionRate: Double = 0.15, extras: Double = 0.0): PricingDetails {
            return if (durationHours != null && durationHours > 0) {
                val base = rate * durationHours
                val comm = base * commissionRate
                val net = (base - comm) + extras
                PricingDetails(
                    hourlyRate = rate,
                    requestedDurationHours = durationHours,
                    estimatedTotalAmount = base + extras,
                    platformCommissionRate = commissionRate,
                    platformCommissionAmount = comm,
                    partnerEstimatedEarnings = net,
                    isEstimated = false,
                    additionalCharges = extras
                )
            } else {
                PricingDetails(
                    hourlyRate = rate,
                    requestedDurationHours = null,
                    estimatedTotalAmount = rate,
                    platformCommissionRate = commissionRate,
                    platformCommissionAmount = rate * commissionRate,
                    partnerEstimatedEarnings = rate * (1.0 - commissionRate),
                    isEstimated = true,
                    additionalCharges = extras
                )
            }
        }
    }
}

data class WorkOrder(
    val id: String,
    val orderNumber: String,
    val serviceCategory: String = "General Helper",
    val customer: Customer,
    val workDescription: String,
    val voiceMessage: VoiceMessage? = null,
    val pricing: PricingDetails = PricingDetails.calculate(),
    val status: OrderStatus = OrderStatus.OFFERED,
    val createdAt: Long = System.currentTimeMillis(),
    val offerExpiresAt: Long = System.currentTimeMillis() + 60_000L,
    val otpCode: String = "4821",
    val customerNotes: String? = null,
    val liveDistanceKm: Double? = null,
    val liveEstimatedTravelTimeMinutes: Int? = null,
    val partnerAcceptedId: String? = null,
    val rejectionReason: String? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null
)

data class WorkCategory(
    val id: String,
    val name: String,
    val baseRate: Double,
    val rateUnit: String = "hour",
    val description: String,
    val iconKey: String = "helper"
)

data class AadhaarVerificationData(
    val maskedNumber: String, // e.g. "XXXX XXXX 1234"
    val fullNameOnAadhaar: String,
    val isDigiLocker: Boolean = false,
    val documentRef: String? = null,
    val status: String = "PENDING",
    val submittedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null
)
typealias AadhaarDetails = AadhaarVerificationData

data class PanVerificationData(
    val maskedNumber: String, // e.g. "ABCDE****F"
    val fullNameOnPan: String,
    val documentRef: String? = null,
    val status: String = "PENDING",
    val submittedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null
)
typealias PanDetails = PanVerificationData

data class LivePhotoData(
    val photoRef: String? = null,
    val status: String = "PENDING",
    val capturedAt: Long = System.currentTimeMillis(),
    val isLivenessChecked: Boolean = false
)

data class PartnerAccount(
    val id: String,
    val mobileNumber: String,
    val legalName: String,
    val email: String? = null,
    val dob: String? = null,
    val address: String,
    val serviceArea: String,
    val emergencyContact: String? = null,
    val status: PartnerAccountStatus = PartnerAccountStatus.PENDING_VERIFICATION,
    val selectedCategories: List<String> = listOf("CAT_GEN_HELPER"),
    val aadhaarData: AadhaarVerificationData? = null,
    val panData: PanVerificationData? = null,
    val livePhotoData: LivePhotoData? = null,
    val rejectionReason: String? = null,
    val actionRequiredMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null,
    val reviewedBy: String? = null,
    val sessionToken: String? = null,
    val isOnline: Boolean = true,
    val currentLat: Double = 19.0760,
    val currentLng: Double = 72.8777,
    val walletBalance: Double = 2450.0,
    val todayEarnings: Double = 890.0,
    val rating: Double = 4.9,
    val totalJobsCompleted: Int = 142
)

data class PartnerRegistrationDraft(
    val legalName: String = "",
    val mobileNumber: String = "",
    val isMobileOtpVerified: Boolean = false,
    val email: String = "",
    val dob: String = "",
    val address: String = "",
    val serviceArea: String = "Andheri West, Mumbai",
    val emergencyContact: String = "",
    val selectedCategoryIds: Set<String> = setOf("CAT_GEN_HELPER"),
    val aadhaarMasked: String = "",
    val aadhaarName: String = "",
    val aadhaarDigiLockerSelected: Boolean = false,
    val aadhaarDocUploaded: Boolean = false,
    val aadhaarConsentAccepted: Boolean = false,
    val panMasked: String = "",
    val panName: String = "",
    val panDocUploaded: Boolean = false,
    val livePhotoBase64OrRef: String? = null,
    val isLivePhotoCaptured: Boolean = false,
    val termsAccepted: Boolean = false
)

data class PartnerProfile(
    val id: String = "PARTNER-1029",
    val name: String = "Ramesh Kumar",
    val phone: String = "+91 98765 43210",
    val avatarUrl: String? = null,
    val rating: Double = 4.9,
    val totalJobsCompleted: Int = 142,
    val isOnline: Boolean = true,
    val currentLatitude: Double = 19.0760,
    val currentLongitude: Double = 72.8777,
    val kycVerified: Boolean = true,
    val bankAccountVerified: Boolean = true,
    val walletBalance: Double = 2450.0,
    val todayEarnings: Double = 890.0,
    val acceptanceRate: Int = 96,
    val accountStatus: PartnerAccountStatus = PartnerAccountStatus.APPROVED
)

enum class RejectionReasonOption(val label: String) {
    TOO_FAR("Customer location is too far"),
    BUSY("Currently occupied with another task"),
    WRONG_SKILL("Requires different equipment/skills"),
    PERSONAL_EMERGENCY("Personal emergency / going offline"),
    LOW_DURATION("Work duration is too short"),
    OTHER("Other reason")
}
