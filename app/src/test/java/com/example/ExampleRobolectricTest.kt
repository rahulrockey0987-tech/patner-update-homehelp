package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PartnerAccountStatus
import com.example.data.model.PartnerRegistrationDraft
import com.example.data.model.PricingDetails
import com.example.data.repository.AcceptResult
import com.example.data.repository.AuthResult
import com.example.data.repository.HomeHelpRepository
import com.example.util.LocationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HOMEHELP Partner", appName)
    }

    @Test
    fun testHelperPricingCalculation() {
        // General Helper at ₹150/hour for 2 hours
        val pricing = PricingDetails.calculate(
            rate = 150.0,
            durationHours = 2.0,
            commissionRate = 0.15
        )

        assertEquals(150.0, pricing.hourlyRate, 0.01)
        assertEquals(2.0, pricing.requestedDurationHours ?: 0.0, 0.01)
        assertEquals(300.0, pricing.estimatedTotalAmount, 0.01)
        assertEquals(45.0, pricing.platformCommissionAmount, 0.01)
        assertEquals(255.0, pricing.partnerEstimatedEarnings, 0.01)
    }

    @Test
    fun testLiveDistanceCalculation() {
        val startLat = 19.0760
        val startLng = 72.8777
        val destLat = 19.0760 + 0.018
        val destLng = 72.8777 + 0.012

        val distResult = LocationHelper.calculateDistance(startLat, startLng, destLat, destLng)
        assertTrue(distResult.distanceKm > 1.0)
        assertTrue(distResult.estimatedMinutes >= 3)
    }

    @Test
    fun testRegistrationAndDuplicateMobilePrevention() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HomeHelpRepository(context)

        val uniqueMobile = "+91 99000 11223"
        val draft = PartnerRegistrationDraft(
            legalName = "Karan Patel",
            mobileNumber = uniqueMobile,
            isMobileOtpVerified = true,
            address = "Flat 101, Bandra West",
            serviceArea = "Bandra, Mumbai",
            selectedCategoryIds = setOf("CAT_GEN_HELPER", "CAT_FURNITURE"),
            aadhaarMasked = "XXXX XXXX 9988",
            aadhaarName = "Karan Patel",
            aadhaarConsentAccepted = true,
            panMasked = "ABCDE1234F",
            panName = "Karan Patel",
            isLivePhotoCaptured = true,
            termsAccepted = true
        )

        // 1. First registration should succeed with PENDING_VERIFICATION
        val res1 = repository.registerPartner(draft)
        assertTrue(res1 is AuthResult.Success)
        val account = (res1 as AuthResult.Success).account
        assertEquals(PartnerAccountStatus.PENDING_VERIFICATION, account.status)
        assertEquals("Karan Patel", account.legalName)

        // 2. Second registration with same mobile must fail with DuplicateMobile error
        val res2 = repository.registerPartner(draft)
        assertTrue(res2 is AuthResult.DuplicateMobile)
    }

    @Test
    fun testLoginAndAdminApprovalWorkflow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HomeHelpRepository(context)

        val mobile = "+91 98999 55443"
        val draft = PartnerRegistrationDraft(
            legalName = "Deepak Verma",
            mobileNumber = mobile,
            isMobileOtpVerified = true,
            address = "Andheri East",
            serviceArea = "Andheri, Mumbai",
            selectedCategoryIds = setOf("CAT_GEN_HELPER"),
            aadhaarConsentAccepted = true,
            termsAccepted = true
        )
        val regRes = repository.registerPartner(draft)
        assertTrue(regRes is AuthResult.Success)
        val partnerId = (regRes as AuthResult.Success).account.id

        // Unapproved partner cannot accept incoming jobs
        repository.simulateNewIncomingOrder("Client", "Move table", "Voice", 1.0)
        val order = repository.incomingOrder.value!!
        val acceptAttempt1 = repository.acceptOrder(order.id)
        assertTrue(acceptAttempt1 is AcceptResult.Error)

        // Admin approves the partner
        repository.adminApprovePartner(partnerId, "SuperAdmin")
        val refreshed = repository.refreshCurrentAccount()
        assertNotNull(refreshed)
        assertEquals(PartnerAccountStatus.APPROVED, refreshed!!.status)

        // Now approved partner CAN accept the active job
        val freshOrder = repository.incomingOrder.value
        assertNotNull(freshOrder)
        val acceptAttempt2 = repository.acceptOrder(freshOrder!!.id)
        assertTrue(acceptAttempt2 is AcceptResult.Success)

        // Test logout clears session
        repository.logout()
        assertNull(repository.currentAccount.value)
    }
}
