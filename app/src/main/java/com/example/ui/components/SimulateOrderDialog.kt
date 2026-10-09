package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HomeHelpNavy
import com.example.ui.theme.Slate600
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald

@Composable
fun SimulateOrderDialog(
    currentOrderId: String?,
    onDismiss: () -> Unit,
    onSimulateOrder: (name: String, desc: String, voice: String, hours: Double) -> Unit,
    onSimulateCompetingClaim: (orderId: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Order Dispatch Simulator",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Test incoming voice orders, loud alarm alerts, and race condition prevention:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scenario 1
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSimulateOrder(
                                "Pooja Sharma",
                                "Move heavy furniture (wardrobe & dining table) between bedrooms and living room.",
                                "“Namaste, please help me move furniture from one room to another, especially the heavy wardrobe.”",
                                2.0
                            )
                            onDismiss()
                        }
                        .padding(bottom = 8.dp)
                        .testTag("simulate_furniture_order"),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🛋️ Furniture Move (2 Hours)", fontWeight = FontWeight.Bold, color = HomeHelpNavy)
                        Text("Customer: Pooja Sharma • ₹150/hr = ₹300 total", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Scenario 2
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSimulateOrder(
                                "Sunil Verma",
                                "Shifting heavy cartons and cleaning balcony & storeroom.",
                                "“Hello, I need helper urgently for shifting 8 heavy boxes. Please accept quickly.”",
                                3.0
                            )
                            onDismiss()
                        }
                        .padding(bottom = 8.dp)
                        .testTag("simulate_shifting_order"),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📦 Heavy Box Shifting (3 Hours)", fontWeight = FontWeight.Bold, color = StatusEmerald)
                        Text("Customer: Sunil Verma • ₹150/hr = ₹450 total", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Scenario 3: Competing partner test
                if (currentOrderId != null) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSimulateCompetingClaim(currentOrderId)
                                onDismiss()
                            }
                            .testTag("simulate_competing_claim"),
                        colors = CardDefaults.elevatedCardColors(containerColor = StatusCrimson.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("⚡ Simulate Competing Partner Claim", fontWeight = FontWeight.Bold, color = StatusCrimson)
                            Text("Simulates another partner accepting Order #$currentOrderId first. When you tap Accept, app will reject double-acceptance gracefully.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Close", color = HomeHelpNavy)
            }
        }
    )
}
