package com.example.ui.screens.auth

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartnerRegistrationDraft
import com.example.data.model.WorkCategory
import com.example.ui.theme.HomeHelpGold
import com.example.ui.theme.HomeHelpNavy
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.White

@Composable
fun RegisterWizardScreen(
    currentStep: Int,
    draft: PartnerRegistrationDraft,
    categories: List<WorkCategory>,
    isProcessing: Boolean,
    onUpdateDraft: ((PartnerRegistrationDraft) -> PartnerRegistrationDraft) -> Unit,
    onSendOtp: (String) -> String,
    onVerifyOtp: (String, String) -> Boolean,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onSubmitRegistration: () -> Unit,
    onCancelToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stepValidationError by remember { mutableStateOf<String?>(null) }
    var capturedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Camera launcher for live photo capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedPhotoBitmap = bitmap
            onUpdateDraft {
                it.copy(
                    livePhotoBase64OrRef = "secure://live_camera_capture_${System.currentTimeMillis()}.jpg",
                    isLivePhotoCaptured = true
                )
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP STEPPER HEADER
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = HomeHelpNavy,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        if (currentStep > 1) onPrevStep() else onCancelToLogin()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PARTNER ONBOARDING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = HomeHelpGold
                        )
                        Text(
                            text = when (currentStep) {
                                1 -> "Step 1 of 6: Personal Details"
                                2 -> "Step 2 of 6: Work Categories"
                                3 -> "Step 3 of 6: Aadhaar Verification"
                                4 -> "Step 4 of 6: PAN Verification"
                                5 -> "Step 5 of 6: Live Photo Capture"
                                else -> "Step 6 of 6: Review & Submit"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = White
                        )
                    }

                    TextButton(onClick = onCancelToLogin) {
                        Text("Sign In", color = Slate200, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / 6f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = HomeHelpGold,
                    trackColor = White.copy(alpha = 0.2f)
                )
            }
        }

        // SCROLLABLE STEP CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (currentStep) {
                1 -> Step1PersonalDetails(
                    draft = draft,
                    onUpdateDraft = onUpdateDraft,
                    onSendOtp = onSendOtp,
                    onVerifyOtp = onVerifyOtp
                )
                2 -> Step2WorkCategories(
                    draft = draft,
                    categories = categories,
                    onUpdateDraft = onUpdateDraft
                )
                3 -> Step3AadhaarVerification(
                    draft = draft,
                    onUpdateDraft = onUpdateDraft
                )
                4 -> Step4PanVerification(
                    draft = draft,
                    onUpdateDraft = onUpdateDraft
                )
                5 -> Step5LivePhotoCapture(
                    draft = draft,
                    photoBitmap = capturedPhotoBitmap,
                    onCaptureClicked = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
                6 -> Step6ReviewAndSubmit(
                    draft = draft,
                    categories = categories,
                    onUpdateDraft = onUpdateDraft
                )
            }

            if (stepValidationError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusCrimson.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ $stepValidationError",
                        color = StatusCrimson,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // BOTTOM NAVIGATION ACTION BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = onPrevStep,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(0.4f)
                            .height(50.dp)
                            .testTag("wizard_prev_button")
                    ) {
                        Text("Previous", color = HomeHelpNavy)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Button(
                    onClick = {
                        // Validate current step
                        val error = validateStep(currentStep, draft)
                        if (error != null) {
                            stepValidationError = error
                        } else {
                            stepValidationError = null
                            if (currentStep < 6) {
                                onNextStep()
                            } else {
                                onSubmitRegistration()
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == 6) StatusEmerald else HomeHelpNavy
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(if (currentStep > 1) 0.6f else 1f)
                        .height(50.dp)
                        .testTag(if (currentStep == 6) "submit_registration_button" else "wizard_next_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submitting...", color = White, fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            text = if (currentStep == 6) "Submit for Verification" else "Continue",
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentStep < 6) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun validateStep(step: Int, draft: PartnerRegistrationDraft): String? {
    return when (step) {
        1 -> {
            if (draft.legalName.isBlank()) "Full legal name is required."
            else if (draft.mobileNumber.trim().length < 10) "Valid 10-digit mobile number required."
            else if (!draft.isMobileOtpVerified) "Please verify your mobile number with OTP first."
            else if (draft.address.isBlank()) "Residential address is required."
            else null
        }
        2 -> {
            if (draft.selectedCategoryIds.isEmpty()) "Please select at least one work category."
            else null
        }
        3 -> {
            if (draft.aadhaarMasked.isBlank()) "Aadhaar number is required."
            else if (!draft.aadhaarConsentAccepted) "You must accept the Aadhaar verification consent."
            else null
        }
        4 -> {
            if (draft.panMasked.isBlank()) "PAN card number is required."
            else null
        }
        5 -> {
            if (!draft.isLivePhotoCaptured) "Please capture a live photo using the camera."
            else null
        }
        6 -> {
            if (!draft.termsAccepted) "Please accept the terms and conditions and privacy notice."
            else null
        }
        else -> null
    }
}

// STEP 1: PERSONAL DETAILS
@Composable
fun Step1PersonalDetails(
    draft: PartnerRegistrationDraft,
    onUpdateDraft: ((PartnerRegistrationDraft) -> PartnerRegistrationDraft) -> Unit,
    onSendOtp: (String) -> String,
    onVerifyOtp: (String, String) -> Boolean
) {
    var otpSent by remember { mutableStateOf(false) }
    var enteredOtp by remember { mutableStateOf("4821") }
    var otpStatusMessage by remember { mutableStateOf<String?>(null) }

    Column {
        Text("Personal Information", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text("Provide legal details as per your government ID.", style = MaterialTheme.typography.bodySmall, color = Slate600)

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = draft.legalName,
            onValueChange = { onUpdateDraft { d -> d.copy(legalName = it) } },
            label = { Text("Full Legal Name *") },
            placeholder = { Text("e.g. Ramesh Kumar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("reg_legal_name_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Mobile & OTP section
        OutlinedTextField(
            value = draft.mobileNumber,
            onValueChange = { onUpdateDraft { d -> d.copy(mobileNumber = it, isMobileOtpVerified = false) } },
            label = { Text("Mobile Number (10 Digits) *") },
            placeholder = { Text("+91 98765 43210") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            trailingIcon = {
                if (draft.isMobileOtpVerified) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = StatusEmerald)
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("reg_mobile_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (!draft.isMobileOtpVerified) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!otpSent) {
                    Button(
                        onClick = {
                            if (draft.mobileNumber.length >= 10) {
                                onSendOtp(draft.mobileNumber)
                                otpSent = true
                                otpStatusMessage = "OTP sent. Enter 4821 below."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HomeHelpNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Send OTP", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { enteredOtp = it },
                        label = { Text("Enter OTP") },
                        modifier = Modifier.width(140.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val ok = onVerifyOtp(draft.mobileNumber, enteredOtp)
                            if (ok) {
                                otpStatusMessage = "Mobile verified!"
                            } else {
                                otpStatusMessage = "Invalid OTP. Use 4821 or 1234."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Verify OTP", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (otpStatusMessage != null) {
                Text(
                    text = otpStatusMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (draft.isMobileOtpVerified) StatusEmerald else Slate600
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.email,
            onValueChange = { onUpdateDraft { d -> d.copy(email = it) } },
            label = { Text("Email Address (Optional)") },
            placeholder = { Text("partner@example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.dob,
            onValueChange = { onUpdateDraft { d -> d.copy(dob = it) } },
            label = { Text("Date of Birth (DD/MM/YYYY) *") },
            placeholder = { Text("15/08/1994") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.address,
            onValueChange = { onUpdateDraft { d -> d.copy(address = it) } },
            label = { Text("Residential Address *") },
            placeholder = { Text("Flat/Room, Building, Area, City") },
            modifier = Modifier.fillMaxWidth().testTag("reg_address_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.serviceArea,
            onValueChange = { onUpdateDraft { d -> d.copy(serviceArea = it) } },
            label = { Text("Preferred Service Area *") },
            placeholder = { Text("e.g. Andheri West, Juhu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.emergencyContact,
            onValueChange = { onUpdateDraft { d -> d.copy(emergencyContact = it) } },
            label = { Text("Emergency Contact Number") },
            placeholder = { Text("+91 98000 00000") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// STEP 2: WORK CATEGORIES
@Composable
fun Step2WorkCategories(
    draft: PartnerRegistrationDraft,
    categories: List<WorkCategory>,
    onUpdateDraft: ((PartnerRegistrationDraft) -> PartnerRegistrationDraft) -> Unit
) {
    Column {
        Text("Select Work Categories", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(
            text = "Select all services you can perform. You will only receive bookings matching your approved categories.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate600
        )

        Spacer(modifier = Modifier.height(14.dp))

        categories.forEach { cat ->
            val isSelected = draft.selectedCategoryIds.contains(cat.id)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable {
                        val newSet = draft.selectedCategoryIds.toMutableSet()
                        if (isSelected) {
                            if (newSet.size > 1) newSet.remove(cat.id)
                        } else {
                            newSet.add(cat.id)
                        }
                        onUpdateDraft { it.copy(selectedCategoryIds = newSet) }
                    }
                    .testTag("cat_card_${cat.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected) HomeHelpGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(if (isSelected) 3.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            val newSet = draft.selectedCategoryIds.toMutableSet()
                            if (checked) newSet.add(cat.id) else if (newSet.size > 1) newSet.remove(cat.id)
                            onUpdateDraft { it.copy(selectedCategoryIds = newSet) }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = HomeHelpNavy)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) HomeHelpNavy else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = cat.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) HomeHelpNavy else Slate100
                    ) {
                        Text(
                            text = "₹${cat.baseRate.toInt()}/${cat.rateUnit}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) White else HomeHelpNavy,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// STEP 3: AADHAAR VERIFICATION
@Composable
fun Step3AadhaarVerification(
    draft: PartnerRegistrationDraft,
    onUpdateDraft: ((PartnerRegistrationDraft) -> PartnerRegistrationDraft) -> Unit
) {
    Column {
        Text("Aadhaar Identity Verification", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(
            text = "Required by government regulations to verify partner identity and protect platform safety.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate600
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Method Choice: DigiLocker or Upload
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { onUpdateDraft { it.copy(aadhaarDigiLockerSelected = true) } },
                shape = RoundedCornerShape(10.dp),
                color = if (draft.aadhaarDigiLockerSelected) HomeHelpNavy else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = draft.aadhaarDigiLockerSelected,
                        onClick = { onUpdateDraft { it.copy(aadhaarDigiLockerSelected = true) } }
                    )
                    Text("DigiLocker (Fast)", color = if (draft.aadhaarDigiLockerSelected) White else MaterialTheme.colorScheme.onSurface)
                }
            }

            Surface(
                onClick = { onUpdateDraft { it.copy(aadhaarDigiLockerSelected = false) } },
                shape = RoundedCornerShape(10.dp),
                color = if (!draft.aadhaarDigiLockerSelected) HomeHelpNavy else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !draft.aadhaarDigiLockerSelected,
                        onClick = { onUpdateDraft { it.copy(aadhaarDigiLockerSelected = false) } }
                    )
                    Text("Secure Upload", color = if (!draft.aadhaarDigiLockerSelected) White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = draft.aadhaarMasked.ifEmpty { "XXXX XXXX 4821" },
            onValueChange = { onUpdateDraft { d -> d.copy(aadhaarMasked = it) } },
            label = { Text("Aadhaar Number (Masked) *") },
            placeholder = { Text("XXXX XXXX 1234") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("reg_aadhaar_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.aadhaarName.ifEmpty { draft.legalName },
            onValueChange = { onUpdateDraft { d -> d.copy(aadhaarName = it) } },
            label = { Text("Name as printed on Aadhaar *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy Box
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Document Privacy & Data Protection", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aadhaar numbers are masked in all logs and databases. Documents are encrypted at rest and accessible only by authorized verification officers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = draft.aadhaarConsentAccepted,
                onCheckedChange = { onUpdateDraft { d -> d.copy(aadhaarConsentAccepted = it) } },
                colors = CheckboxDefaults.colors(checkedColor = HomeHelpNavy),
                modifier = Modifier.testTag("aadhaar_consent_checkbox")
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "I consent to identity verification for HOMEHELP Partner onboarding.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// STEP 4: PAN VERIFICATION
@Composable
fun Step4PanVerification(
    draft: PartnerRegistrationDraft,
    onUpdateDraft: ((PartnerRegistrationDraft) -> PartnerRegistrationDraft) -> Unit
) {
    Column {
        Text("PAN Card Verification", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(
            text = "Required for daily payout transfers and tax compliance.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate600
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = draft.panMasked.ifEmpty { "ABCDE1234F" },
            onValueChange = { onUpdateDraft { d -> d.copy(panMasked = it.uppercase()) } },
            label = { Text("PAN Number (10 Characters) *") },
            placeholder = { Text("ABCDE1234F") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("reg_pan_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = draft.panName.ifEmpty { draft.legalName },
            onValueChange = { onUpdateDraft { d -> d.copy(panName = it) } },
            label = { Text("Name on PAN Card *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = HomeHelpNavy, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("PAN Document File", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(if (draft.panDocUploaded) "Document attached & encrypted" else "PDF/JPEG format", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    }
                }

                Button(
                    onClick = { onUpdateDraft { it.copy(panDocUploaded = true) } },
                    colors = ButtonDefaults.buttonColors(containerColor = if (draft.panDocUploaded) StatusEmerald else HomeHelpNavy),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (draft.panDocUploaded) "Attached" else "Attach Doc", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// STEP 5: LIVE PHOTO CAPTURE
@Composable
fun Step5LivePhotoCapture(
    draft: PartnerRegistrationDraft,
    photoBitmap: Bitmap?,
    onCaptureClicked: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Live Camera Photo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(
            text = "Take a fresh selfie photo using your device camera. Gallery uploads are prohibited for identity safety.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate600
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Camera Preview / Snapshot Box
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Slate100)
                .border(3.dp, if (draft.isLivePhotoCaptured) StatusEmerald else HomeHelpNavy, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = "Captured Photo",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = Slate500,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No photo taken yet", style = MaterialTheme.typography.labelSmall, color = Slate500)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onCaptureClicked,
            colors = ButtonDefaults.buttonColors(containerColor = HomeHelpNavy),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("take_live_photo_button")
        ) {
            Icon(
                imageVector = if (draft.isLivePhotoCaptured) Icons.Default.Refresh else Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (draft.isLivePhotoCaptured) "Retake Fresh Photo" else "Open Camera & Capture")
        }

        if (draft.isLivePhotoCaptured) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Live camera photo verified and ready", color = StatusEmerald, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// STEP 6: REVIEW & SUBMIT
@Composable
fun Step6ReviewAndSubmit(
    draft: PartnerRegistrationDraft,
    categories: List<WorkCategory>,
    onUpdateDraft: ((PartnerRegistrationDraft) -> PartnerRegistrationDraft) -> Unit
) {
    val selectedCategoryNames = categories
        .filter { draft.selectedCategoryIds.contains(it.id) }
        .map { it.name }

    Column {
        Text("Application Summary", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text("Review your details before submitting for Admin approval.", style = MaterialTheme.typography.bodySmall, color = Slate600)

        Spacer(modifier = Modifier.height(14.dp))

        // Summary Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ReviewRow("Legal Name", draft.legalName)
                ReviewRow("Mobile", draft.mobileNumber + " (Verified)")
                ReviewRow("Address", draft.address)
                ReviewRow("Service Area", draft.serviceArea)
                ReviewRow("Categories", selectedCategoryNames.joinToString(", "))
                ReviewRow("Aadhaar", draft.aadhaarMasked + " (Consent Given)")
                ReviewRow("PAN Card", draft.panMasked)
                ReviewRow("Live Photo", if (draft.isLivePhotoCaptured) "Captured ✓" else "Pending")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Terms acceptance
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = draft.termsAccepted,
                onCheckedChange = { onUpdateDraft { d -> d.copy(termsAccepted = it) } },
                colors = CheckboxDefaults.colors(checkedColor = HomeHelpNavy),
                modifier = Modifier.testTag("terms_checkbox")
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "I hereby confirm that all information provided is accurate and true. I agree to the HOMEHELP Partner Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Slate600)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
