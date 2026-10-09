package com.example.ui.screens.profile

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PartnerProfile
import com.example.ui.theme.HomeHelpGold
import com.example.ui.theme.HomeHelpNavy
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.White

@Composable
fun ProfileScreen(
    profile: PartnerProfile,
    onToggleOnline: (Boolean) -> Unit,
    onOpenSimulator: () -> Unit,
    onLogout: () -> Unit,
    onOpenAdminPortal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Partner Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Partner Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HomeHelpNavy.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = HomeHelpNavy,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = StatusEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "ID: ${profile.id} • ${profile.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = HomeHelpGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${profile.rating} Rating • ${profile.totalJobsCompleted} Completed Tasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = HomeHelpNavy
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ONLINE / OFFLINE TOGGLE CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (profile.isOnline) StatusEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (profile.isOnline) "You are ONLINE" else "You are OFFLINE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (profile.isOnline) StatusEmerald else Slate600
                    )
                    Text(
                        text = if (profile.isOnline) "Ready to receive incoming voice orders & alerts" else "Turn on to start receiving nearby job requests",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }

                Switch(
                    checked = profile.isOnline,
                    onCheckedChange = onToggleOnline,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = StatusEmerald
                    ),
                    modifier = Modifier.testTag("online_offline_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Service Details & KYC
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationCity, contentDescription = null, tint = HomeHelpNavy, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Active Service Area", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("Andheri West, Juhu, Bandra (8 km radius)", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate200)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Government KYC", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("Aadhaar & PAN Verified on 14 Jan 2026", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Testing Dispatch Simulator Action
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = HomeHelpNavy.copy(alpha = 0.05f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = HomeHelpNavy, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live Order & Concurrency Testing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Trigger test voice orders, loud alarm chiming, GPS distance verification, and competing partner acceptance race conditions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenSimulator,
                    colors = ButtonDefaults.buttonColors(containerColor = HomeHelpNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("open_simulator_button")
                ) {
                    Text("Open Order Dispatch Simulator", color = White)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Admin Portal Link
        OutlinedButton(
            onClick = onOpenAdminPortal,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("profile_admin_portal_button")
        ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = HomeHelpNavy)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Admin Portal: Verify & Approve Partners", color = HomeHelpNavy)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Logout Button
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = StatusCrimson.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("partner_logout_button")
        ) {
            Text("Logout & Exit Session", color = StatusCrimson, fontWeight = FontWeight.Bold)
        }
    }
}
