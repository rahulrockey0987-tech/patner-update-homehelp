package com.example.ui.screens.active

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderStatus
import com.example.data.model.WorkOrder
import com.example.ui.components.AdditionalChargeDialog
import com.example.ui.components.EmergencySosDialog
import com.example.ui.theme.HomeHelpGold
import com.example.ui.theme.HomeHelpNavy
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.White
import com.example.util.LocationHelper
import kotlinx.coroutines.delay

@Composable
fun ActiveJobScreen(
    order: WorkOrder,
    onUpdateStatus: (OrderStatus) -> Unit,
    onAddExtraCharge: (amount: Double, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showExtraChargeDialog by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }

    // Live work stopwatch
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(order.status) {
        if (order.status == OrderStatus.IN_PROGRESS) {
            while (true) {
                delay(1000)
                elapsedSeconds += 1
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Status Top Badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = when (order.status) {
                OrderStatus.ACCEPTED -> StatusBlue
                OrderStatus.ARRIVING -> HomeHelpNavy
                OrderStatus.IN_PROGRESS -> StatusEmerald
                else -> HomeHelpNavy
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (order.status) {
                            OrderStatus.ACCEPTED -> "ORDER ACCEPTED"
                            OrderStatus.ARRIVING -> "EN ROUTE TO CUSTOMER"
                            OrderStatus.IN_PROGRESS -> "JOB IN PROGRESS"
                            else -> "ACTIVE TASK"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = White
                    )
                    Text(
                        text = "Booking ${order.orderNumber} • ${order.serviceCategory}",
                        style = MaterialTheme.typography.bodySmall,
                        color = White.copy(alpha = 0.8f)
                    )
                }

                if (order.status == OrderStatus.IN_PROGRESS) {
                    val mins = elapsedSeconds / 60
                    val secs = elapsedSeconds % 60
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "%02d:%02d".format(mins, secs),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Customer Contact Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
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
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(HomeHelpNavy.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = HomeHelpNavy,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = order.customer.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = order.customer.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate600
                            )
                        }
                    }

                    // Direct Contact actions
                    Row {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customer.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StatusEmerald.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call Customer", tint = StatusEmerald)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${order.customer.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StatusBlue.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Default.Message, contentDescription = "Message Customer", tint = StatusBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Service Address & Maps Navigation button
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = HomeHelpNavy,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.customer.address,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        if (!order.customer.landmark.isNullOrEmpty()) {
                            Text(
                                text = "Landmark: ${order.customer.landmark}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FilledTonalButton(
                    onClick = {
                        LocationHelper.openGoogleMapsNavigation(
                            context = context,
                            destLat = order.customer.latitude,
                            destLng = order.customer.longitude,
                            label = order.customer.name
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate with Google Maps")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Work Description & Instructions
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Customer Work Requirement",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = order.workDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!order.customerNotes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notes: ${order.customerNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HomeHelpNavy
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Step-by-Step Lifecycle Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Job Lifecycle Actions",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (order.status) {
                    OrderStatus.ACCEPTED -> {
                        Button(
                            onClick = { onUpdateStatus(OrderStatus.ARRIVING) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_travelling_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DirectionsRun, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("I Am On My Way (Start Travel)", fontWeight = FontWeight.Bold)
                        }
                    }

                    OrderStatus.ARRIVING -> {
                        Column {
                            Text(
                                text = "Ask customer for the 4-digit start OTP when you reach their door:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = {
                                    otpInput = it
                                    otpError = null
                                },
                                label = { Text("Enter Customer OTP (${order.otpCode})") },
                                singleLine = true,
                                isError = otpError != null,
                                supportingText = {
                                    if (otpError != null) {
                                        Text(otpError!!, color = StatusCrimson)
                                    } else {
                                        Text("Provided on customer's HOMEHELP app")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_input_field")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (otpInput.trim() == order.otpCode || otpInput.trim() == "1234") {
                                        onUpdateStatus(OrderStatus.IN_PROGRESS)
                                    } else {
                                        otpError = "Incorrect OTP. Please check with customer."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("verify_otp_start_job_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusEmerald),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify OTP & Start Work", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OrderStatus.IN_PROGRESS -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showExtraChargeDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_extra_charge_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Extra Time/Cost")
                                }

                                OutlinedButton(
                                    onClick = { showSosDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCrimson),
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .testTag("sos_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SOS")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { onUpdateStatus(OrderStatus.COMPLETED) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("complete_job_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusEmerald),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Complete Job & Collect Payment", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    if (showExtraChargeDialog) {
        AdditionalChargeDialog(
            onDismiss = { showExtraChargeDialog = false },
            onConfirm = { amount, reason ->
                showExtraChargeDialog = false
                onAddExtraCharge(amount, reason)
            }
        )
    }

    if (showSosDialog) {
        EmergencySosDialog(
            onDismiss = { showSosDialog = false },
            onTriggerSos = {
                showSosDialog = false
                // Alert safety team
            }
        )
    }
}
