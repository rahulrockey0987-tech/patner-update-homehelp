package com.example.data.network

import com.example.data.model.PricingDetails
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null,
    val errorCode: String? = null
)

// Auth and Registration DTOs
data class SendOtpRequest(
    val mobileNumber: String
)

data class SendOtpResponse(
    val message: String,
    val tempSessionId: String,
    val expiresAt: Long
)

data class VerifyOtpRequest(
    val mobileNumber: String,
    val otp: String
)

data class RegisterPartnerRequest(
    val legalName: String,
    val mobileNumber: String,
    val otp: String,
    val email: String?,
    val dob: String?,
    val address: String,
    val serviceArea: String,
    val emergencyContact: String?,
    val categoryIds: List<String>,
    val aadhaarMasked: String,
    val aadhaarName: String,
    val panMasked: String,
    val panName: String,
    val livePhotoRef: String?
)

data class LoginRequest(
    val mobileNumber: String,
    val otp: String
)

data class AuthSessionResponse(
    val token: String,
    val partnerId: String,
    val status: String,
    val legalName: String,
    val mobileNumber: String
)

data class WorkCategoryDto(
    val id: String,
    val name: String,
    val baseRate: Double,
    val rateUnit: String = "hour",
    val description: String,
    val iconKey: String = "helper"
)

data class VerificationStatusResponse(
    val partnerId: String,
    val status: String,
    val aadhaarStatus: String,
    val panStatus: String,
    val photoStatus: String,
    val rejectionReason: String? = null,
    val actionRequiredMessage: String? = null,
    val reviewedAt: Long? = null
)

data class AdminReviewRequest(
    val action: String, // "APPROVE", "REJECT", "ACTION_REQUIRED", "SUSPEND"
    val reasonOrMessage: String? = null,
    val reviewerName: String
)

// Order DTOs
data class AcceptOrderRequest(
    val partnerId: String,
    val partnerLat: Double,
    val partnerLng: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class RejectOrderRequest(
    val partnerId: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class UpdateOrderStatusRequest(
    val partnerId: String,
    val status: String,
    val otpCode: String? = null,
    val additionalCharges: Double? = null
)

data class UpdateLocationRequest(
    val partnerId: String,
    val latitude: Double,
    val longitude: Double,
    val isOnline: Boolean
)

data class RemoteOrderDto(
    val id: String,
    val orderNumber: String,
    val serviceCategory: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerAvatarUrl: String? = null,
    val customerAddress: String,
    val customerLandmark: String? = null,
    val customerLatitude: Double,
    val customerLongitude: Double,
    val workDescription: String,
    val voiceAudioUrl: String? = null,
    val voiceDurationSeconds: Int? = null,
    val voiceTranscript: String? = null,
    val hourlyRate: Double = 150.0,
    val requestedDurationHours: Double? = 2.0,
    val estimatedTotalAmount: Double = 300.0,
    val platformCommissionAmount: Double = 45.0,
    val partnerEstimatedEarnings: Double = 255.0,
    val status: String = "OFFERED",
    val offerExpiresAt: Long,
    val otpCode: String = "4821",
    val acceptedPartnerId: String? = null
)

interface HomeHelpApi {
    // Auth & Registration endpoints
    @POST("api/v1/partners/otp/send")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<ApiResponse<SendOtpResponse>>

    @POST("api/v1/partners/otp/verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<Boolean>>

    @POST("api/v1/partners/register")
    suspend fun registerPartner(@Body request: RegisterPartnerRequest): Response<ApiResponse<AuthSessionResponse>>

    @POST("api/v1/partners/login")
    suspend fun loginPartner(@Body request: LoginRequest): Response<ApiResponse<AuthSessionResponse>>

    @POST("api/v1/partners/logout")
    suspend fun logoutPartner(@Header("Authorization") token: String): Response<ApiResponse<Boolean>>

    @GET("api/v1/categories")
    suspend fun getCategories(): Response<ApiResponse<List<WorkCategoryDto>>>

    @GET("api/v1/partners/verification-status")
    suspend fun getVerificationStatus(
        @Header("Authorization") token: String,
        @Query("partnerId") partnerId: String
    ): Response<ApiResponse<VerificationStatusResponse>>

    // Order endpoints
    @GET("api/v1/orders/incoming")
    suspend fun getIncomingOrders(
        @Header("Authorization") token: String,
        @Query("partnerId") partnerId: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<ApiResponse<List<RemoteOrderDto>>>

    @POST("api/v1/orders/{id}/accept")
    suspend fun acceptOrder(
        @Header("Authorization") token: String,
        @Path("id") orderId: String,
        @Body request: AcceptOrderRequest
    ): Response<ApiResponse<RemoteOrderDto>>

    @POST("api/v1/orders/{id}/reject")
    suspend fun rejectOrder(
        @Header("Authorization") token: String,
        @Path("id") orderId: String,
        @Body request: RejectOrderRequest
    ): Response<ApiResponse<Boolean>>

    @PATCH("api/v1/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Header("Authorization") token: String,
        @Path("id") orderId: String,
        @Body request: UpdateOrderStatusRequest
    ): Response<ApiResponse<RemoteOrderDto>>

    @PATCH("api/v1/partner/location")
    suspend fun updateLocation(
        @Header("Authorization") token: String,
        @Body request: UpdateLocationRequest
    ): Response<ApiResponse<Boolean>>
}
