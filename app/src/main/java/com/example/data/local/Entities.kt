package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AadhaarVerificationData
import com.example.data.model.Customer
import com.example.data.model.LivePhotoData
import com.example.data.model.OrderStatus
import com.example.data.model.PanVerificationData
import com.example.data.model.PartnerAccount
import com.example.data.model.PartnerAccountStatus
import com.example.data.model.PricingDetails
import com.example.data.model.VoiceMessage
import com.example.data.model.WorkCategory
import com.example.data.model.WorkOrder

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val serviceCategory: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerAvatarUrl: String?,
    val customerAddress: String,
    val customerLandmark: String?,
    val customerLat: Double,
    val customerLng: Double,
    val customerRating: Double,
    val workDescription: String,
    val voiceAudioUrl: String?,
    val voiceDurationSeconds: Int?,
    val voiceTranscript: String?,
    val hourlyRate: Double,
    val requestedDurationHours: Double?,
    val estimatedTotalAmount: Double,
    val platformCommissionAmount: Double,
    val partnerEstimatedEarnings: Double,
    val status: String,
    val createdAt: Long,
    val offerExpiresAt: Long,
    val otpCode: String,
    val startedAt: Long?,
    val completedAt: Long?,
    val rejectionReason: String?
) {
    fun toDomain(): WorkOrder {
        val cust = Customer(
            id = customerId,
            name = customerName,
            phone = customerPhone,
            avatarUrl = customerAvatarUrl,
            rating = customerRating,
            address = customerAddress,
            landmark = customerLandmark,
            latitude = customerLat,
            longitude = customerLng
        )
        val vm = if (!voiceAudioUrl.isNullOrEmpty()) {
            VoiceMessage(
                audioUrl = voiceAudioUrl,
                durationSeconds = voiceDurationSeconds ?: 12,
                transcript = voiceTranscript
            )
        } else null

        val pricing = PricingDetails(
            hourlyRate = hourlyRate,
            requestedDurationHours = requestedDurationHours,
            estimatedTotalAmount = estimatedTotalAmount,
            platformCommissionRate = 0.15,
            platformCommissionAmount = platformCommissionAmount,
            partnerEstimatedEarnings = partnerEstimatedEarnings,
            isEstimated = requestedDurationHours == null
        )

        val parsedStatus = try {
            OrderStatus.valueOf(status)
        } catch (_: Exception) {
            OrderStatus.PENDING
        }

        return WorkOrder(
            id = id,
            orderNumber = orderNumber,
            serviceCategory = serviceCategory,
            customer = cust,
            workDescription = workDescription,
            voiceMessage = vm,
            pricing = pricing,
            status = parsedStatus,
            createdAt = createdAt,
            offerExpiresAt = offerExpiresAt,
            otpCode = otpCode,
            startedAt = startedAt,
            completedAt = completedAt,
            rejectionReason = rejectionReason
        )
    }

    companion object {
        fun fromDomain(order: WorkOrder): OrderEntity {
            return OrderEntity(
                id = order.id,
                orderNumber = order.orderNumber,
                serviceCategory = order.serviceCategory,
                customerId = order.customer.id,
                customerName = order.customer.name,
                customerPhone = order.customer.phone,
                customerAvatarUrl = order.customer.avatarUrl,
                customerAddress = order.customer.address,
                customerLandmark = order.customer.landmark,
                customerLat = order.customer.latitude,
                customerLng = order.customer.longitude,
                customerRating = order.customer.rating,
                workDescription = order.workDescription,
                voiceAudioUrl = order.voiceMessage?.audioUrl,
                voiceDurationSeconds = order.voiceMessage?.durationSeconds,
                voiceTranscript = order.voiceMessage?.transcript,
                hourlyRate = order.pricing.hourlyRate,
                requestedDurationHours = order.pricing.requestedDurationHours,
                estimatedTotalAmount = order.pricing.estimatedTotalAmount,
                platformCommissionAmount = order.pricing.platformCommissionAmount,
                partnerEstimatedEarnings = order.pricing.partnerEstimatedEarnings,
                status = order.status.name,
                createdAt = order.createdAt,
                offerExpiresAt = order.offerExpiresAt,
                otpCode = order.otpCode,
                startedAt = order.startedAt,
                completedAt = order.completedAt,
                rejectionReason = order.rejectionReason
            )
        }
    }
}

@Entity(tableName = "partner_accounts")
data class PartnerAccountEntity(
    @PrimaryKey val id: String,
    val mobileNumber: String,
    val legalName: String,
    val email: String?,
    val dob: String?,
    val address: String,
    val serviceArea: String,
    val emergencyContact: String?,
    val status: String,
    val selectedCategoriesJoined: String, // Comma separated IDs
    val aadhaarMasked: String?,
    val aadhaarName: String?,
    val aadhaarStatus: String?,
    val panMasked: String?,
    val panName: String?,
    val panStatus: String?,
    val livePhotoRef: String?,
    val livePhotoStatus: String?,
    val rejectionReason: String?,
    val actionRequiredMessage: String?,
    val createdAt: Long,
    val approvedAt: Long?,
    val reviewedBy: String?,
    val sessionToken: String?
) {
    fun toDomain(): PartnerAccount {
        val parsedStatus = try {
            PartnerAccountStatus.valueOf(status)
        } catch (_: Exception) {
            PartnerAccountStatus.PENDING_VERIFICATION
        }

        val aadhaar = if (!aadhaarMasked.isNullOrEmpty()) {
            com.example.data.model.AadhaarVerificationData(
                maskedNumber = aadhaarMasked,
                fullNameOnAadhaar = aadhaarName ?: legalName,
                status = aadhaarStatus ?: "PENDING"
            )
        } else null

        val pan = if (!panMasked.isNullOrEmpty()) {
            com.example.data.model.PanVerificationData(
                maskedNumber = panMasked,
                fullNameOnPan = panName ?: legalName,
                status = panStatus ?: "PENDING"
            )
        } else null

        val photo = if (!livePhotoRef.isNullOrEmpty()) {
            com.example.data.model.LivePhotoData(
                photoRef = livePhotoRef,
                status = livePhotoStatus ?: "PENDING"
            )
        } else null

        val categories = if (selectedCategoriesJoined.isNotEmpty()) {
            selectedCategoriesJoined.split(",").filter { it.isNotBlank() }
        } else listOf("CAT_GEN_HELPER")

        return PartnerAccount(
            id = id,
            mobileNumber = mobileNumber,
            legalName = legalName,
            email = email,
            dob = dob,
            address = address,
            serviceArea = serviceArea,
            emergencyContact = emergencyContact,
            status = parsedStatus,
            selectedCategories = categories,
            aadhaarData = aadhaar,
            panData = pan,
            livePhotoData = photo,
            rejectionReason = rejectionReason,
            actionRequiredMessage = actionRequiredMessage,
            createdAt = createdAt,
            approvedAt = approvedAt,
            reviewedBy = reviewedBy,
            sessionToken = sessionToken
        )
    }

    companion object {
        fun fromDomain(account: PartnerAccount): PartnerAccountEntity {
            return PartnerAccountEntity(
                id = account.id,
                mobileNumber = account.mobileNumber,
                legalName = account.legalName,
                email = account.email,
                dob = account.dob,
                address = account.address,
                serviceArea = account.serviceArea,
                emergencyContact = account.emergencyContact,
                status = account.status.name,
                selectedCategoriesJoined = account.selectedCategories.joinToString(","),
                aadhaarMasked = account.aadhaarData?.maskedNumber,
                aadhaarName = account.aadhaarData?.fullNameOnAadhaar,
                aadhaarStatus = account.aadhaarData?.status,
                panMasked = account.panData?.maskedNumber,
                panName = account.panData?.fullNameOnPan,
                panStatus = account.panData?.status,
                livePhotoRef = account.livePhotoData?.photoRef,
                livePhotoStatus = account.livePhotoData?.status,
                rejectionReason = account.rejectionReason,
                actionRequiredMessage = account.actionRequiredMessage,
                createdAt = account.createdAt,
                approvedAt = account.approvedAt,
                reviewedBy = account.reviewedBy,
                sessionToken = account.sessionToken
            )
        }
    }
}

@Entity(tableName = "work_categories")
data class WorkCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseRate: Double,
    val rateUnit: String,
    val description: String,
    val iconKey: String
) {
    fun toDomain(): WorkCategory = WorkCategory(
        id = id,
        name = name,
        baseRate = baseRate,
        rateUnit = rateUnit,
        description = description,
        iconKey = iconKey
    )

    companion object {
        fun fromDomain(cat: WorkCategory): WorkCategoryEntity = WorkCategoryEntity(
            id = cat.id,
            name = cat.name,
            baseRate = cat.baseRate,
            rateUnit = cat.rateUnit,
            description = cat.description,
            iconKey = cat.iconKey
        )
    }
}

@Entity(tableName = "active_session")
data class SessionEntity(
    @PrimaryKey val id: Int = 1,
    val partnerId: String,
    val sessionToken: String,
    val mobileNumber: String,
    val createdAt: Long = System.currentTimeMillis()
)
