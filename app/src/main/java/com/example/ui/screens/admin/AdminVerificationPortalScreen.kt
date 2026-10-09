package com.example.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartnerAccount
import com.example.data.model.PartnerAccountStatus
import com.example.ui.theme.HomeHelpGold
import com.example.ui.theme.HomeHelpNavy
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminVerificationPortalScreen(
    applications: List<PartnerAccount>,
    onApprove: (partnerId: String) -> Unit,
    onReject: (partnerId: String, reason: String) -> Unit,
    onRequestAction: (partnerId: String, message: String) -> Unit,
    onSuspend: (partnerId: String, reason: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("PENDING") }
    var selectedAppForReview by remember { mutableStateOf<PartnerAccount?>(null) }
    var rejectionDialogPartnerId by remember { mutableStateOf<String?>(null) }
    var actionDialogPartnerId by remember { mutableStateOf<String?>(null) }

    val filteredList = applications.filter {
        when (selectedTab) {
            "PENDING" -> it.status == PartnerAccountStatus.PENDING_VERIFICATION
            "APPROVED" -> it.status == PartnerAccountStatus.APPROVED
            "REJECTED" -> it.status == PartnerAccountStatus.REJECTED || it.status == PartnerAccountStatus.SUSPENDED
            else -> true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = HomeHelpNavy,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "HOMEHELP ADMIN PANEL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = HomeHelpGold
                    )
                    Text(
                        text = "Partner KYC Verification",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = White
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("PENDING" to "Pending Review", "APPROVED" to "Approved", "REJECTED" to "Declined/Suspended", "ALL" to "All").forEach { (key, label) ->
                    FilterChip(
                        selected = selectedTab == key,
                        onClick = { selectedTab = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No partner records in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { app ->
                        AdminPartnerCard(
                            account = app,
                            onApproveClicked = { onApprove(app.id) },
                            onRejectClicked = { rejectionDialogPartnerId = app.id },
                            onRequestActionClicked = { actionDialogPartnerId = app.id },
                            onSuspendClicked = { onSuspend(app.id, "Administrative suspension") }
                        )
                    }
                }
            }
        }
    }

    // Rejection reason dialog
    if (rejectionDialogPartnerId != null) {
        var reasonText by remember { mutableStateOf("Aadhaar details and name mismatch with government database.") }
        AlertDialog(
            onDismissRequest = { rejectionDialogPartnerId = null },
            title = { Text("Reject Partner Application", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter the formal rejection reason to show to the partner:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectionDialogPartnerId!!, reasonText)
                        rejectionDialogPartnerId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCrimson)
                ) {
                    Text("Confirm Rejection", color = White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { rejectionDialogPartnerId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Action required dialog
    if (actionDialogPartnerId != null) {
        var actionText by remember { mutableStateOf("Uploaded document image is blurry. Please re-upload a clear copy.") }
        AlertDialog(
            onDismissRequest = { actionDialogPartnerId = null },
            title = { Text("Request Correction / Resubmission", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Partner will be asked to correct and resubmit:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = actionText,
                        onValueChange = { actionText = it },
                        label = { Text("Instructions for Partner") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRequestAction(actionDialogPartnerId!!, actionText)
                        actionDialogPartnerId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAmber)
                ) {
                    Text("Request Correction", color = White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { actionDialogPartnerId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminPartnerCard(
    account: PartnerAccount,
    onApproveClicked: () -> Unit,
    onRejectClicked: () -> Unit,
    onRequestActionClicked: () -> Unit,
    onSuspendClicked: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HomeHelpNavy.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = HomeHelpNavy, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(account.legalName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("ID: ${account.id} • ${account.mobileNumber}", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    }
                }

                // Status Pill
                val (pillColor, pillText) = when (account.status) {
                    PartnerAccountStatus.APPROVED -> StatusEmerald to "APPROVED"
                    PartnerAccountStatus.PENDING_VERIFICATION -> StatusAmber to "PENDING"
                    PartnerAccountStatus.ACTION_REQUIRED -> StatusAmber to "CORRECTION REQ"
                    PartnerAccountStatus.REJECTED -> StatusCrimson to "REJECTED"
                    PartnerAccountStatus.SUSPENDED -> StatusCrimson to "SUSPENDED"
                    PartnerAccountStatus.DRAFT -> Slate500 to "DRAFT"
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = pillColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = pillText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = pillColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Grid
            Text("Service Area: ${account.serviceArea}", style = MaterialTheme.typography.bodySmall, color = Slate600)
            Text("Address: ${account.address}", style = MaterialTheme.typography.bodySmall, color = Slate600)
            Text("Categories: ${account.selectedCategories.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = HomeHelpNavy)

            Spacer(modifier = Modifier.height(8.dp))

            // Masked Document Info (Protected)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate100,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Aadhaar: ${account.aadhaarData?.maskedNumber ?: "Not Provided"} • Name: ${account.aadhaarData?.fullNameOnAadhaar ?: "-"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "PAN Card: ${account.panData?.maskedNumber ?: "Not Provided"} • Name: ${account.panData?.fullNameOnPan ?: "-"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Live Photo: ${if (account.livePhotoData?.isLivenessChecked == true) "Captured & Verified ✓" else "Attached"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (account.status != PartnerAccountStatus.APPROVED) {
                    Button(
                        onClick = onApproveClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusEmerald),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_approve_${account.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", color = White, fontWeight = FontWeight.Bold)
                    }
                }

                if (account.status == PartnerAccountStatus.PENDING_VERIFICATION) {
                    OutlinedButton(
                        onClick = onRequestActionClicked,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Correction", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = onRejectClicked,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCrimson),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reject", color = StatusCrimson, style = MaterialTheme.typography.labelSmall)
                    }
                } else if (account.status == PartnerAccountStatus.APPROVED) {
                    OutlinedButton(
                        onClick = onSuspendClicked,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCrimson),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suspend", color = StatusCrimson)
                    }
                }
            }
        }
    }
}
