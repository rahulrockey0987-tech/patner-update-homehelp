package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OrderStatus
import com.example.data.model.PartnerAccountStatus
import com.example.ui.components.SimulateOrderDialog
import com.example.ui.screens.active.ActiveJobScreen
import com.example.ui.screens.admin.AdminVerificationPortalScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterWizardScreen
import com.example.ui.screens.earnings.EarningsScreen
import com.example.ui.screens.incoming.IncomingOrderScreen
import com.example.ui.screens.jobs.JobsScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.status.ActionRequiredScreen
import com.example.ui.screens.status.RejectedApplicationScreen
import com.example.ui.screens.status.SuspendedScreen
import com.example.ui.screens.status.VerificationPendingScreen
import com.example.ui.theme.HomeHelpGold
import com.example.ui.theme.HomeHelpNavy
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.White

enum class HomeHelpTab {
    INCOMING_OR_HOME,
    ACTIVE_JOB,
    JOBS,
    EARNINGS,
    PROFILE
}

@Composable
fun HomeHelpPartnerApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentAccount by viewModel.currentAccount.collectAsStateWithLifecycle()
    val registrationDraft by viewModel.registrationDraft.collectAsStateWithLifecycle()
    val registrationStep by viewModel.registrationStep.collectAsStateWithLifecycle()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val pendingApplications by viewModel.pendingPartnerApplications.collectAsStateWithLifecycle()

    val incomingOrder by viewModel.incomingOrder.collectAsStateWithLifecycle()
    val activeJob by viewModel.activeJob.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val profile by viewModel.partnerProfile.collectAsStateWithLifecycle()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsStateWithLifecycle()
    val isAlarmSounding by viewModel.isAlarmSounding.collectAsStateWithLifecycle()
    val isProcessingAction by viewModel.isProcessingAction.collectAsStateWithLifecycle()
    val voicePlayerState by viewModel.voicePlayerState.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()

    var isRegisteringView by remember { mutableStateOf(false) }
    var showAdminPortal by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(HomeHelpTab.INCOMING_OR_HOME) }
    var showSimulatorDialog by remember { mutableStateOf(false) }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setLocationPermissionGranted(granted)
        if (granted) {
            Toast.makeText(context, "Location enabled. Distance will be updated.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission is required for live distance.", Toast.LENGTH_LONG).show()
        }
    }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Notification permission recommended for incoming order alerts.", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-check and request permissions on launch
    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.setLocationPermissionGranted(fineLocationGranted)
        if (!fineLocationGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Listen to UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.OrderAcceptedSuccess -> {
                    selectedTab = HomeHelpTab.ACTIVE_JOB
                    snackbarHostState.showSnackbar("Order ${event.orderNumber} accepted! Navigate to customer.")
                }
                is UiEvent.OrderAlreadyTakenAlert -> {
                    snackbarHostState.showSnackbar("⚠️ ${event.message}")
                }
                is UiEvent.RegistrationSubmitted -> {
                    isRegisteringView = false
                    snackbarHostState.showSnackbar("Application submitted for Admin verification!")
                }
                is UiEvent.LoginSuccess -> {
                    isRegisteringView = false
                    snackbarHostState.showSnackbar("Logged in successfully.")
                }
                is UiEvent.AuthError -> {
                    snackbarHostState.showSnackbar("⚠️ ${event.error}")
                }
            }
        }
    }

    // If an incoming order arrives, auto-switch to Incoming tab
    LaunchedEffect(incomingOrder) {
        if (incomingOrder != null && incomingOrder?.status == OrderStatus.OFFERED && currentAccount?.status == PartnerAccountStatus.APPROVED) {
            selectedTab = HomeHelpTab.INCOMING_OR_HOME
        }
    }

    // TOP-LEVEL SCREEN ROUTING
    if (showAdminPortal) {
        AdminVerificationPortalScreen(
            applications = pendingApplications,
            onApprove = { viewModel.adminApprovePartner(it) },
            onReject = { id, reason -> viewModel.adminRejectPartner(id, reason) },
            onRequestAction = { id, msg -> viewModel.adminRequestAction(id, msg) },
            onSuspend = { id, reason -> viewModel.adminSuspendPartner(id, reason) },
            onBack = { showAdminPortal = false }
        )
    } else if (currentAccount == null) {
        // AUTHENTICATION FLOW: LOGIN OR MULTI-STEP REGISTRATION
        if (isRegisteringView) {
            RegisterWizardScreen(
                currentStep = registrationStep,
                draft = registrationDraft,
                categories = availableCategories,
                isProcessing = isProcessingAction,
                onUpdateDraft = { viewModel.updateRegistrationDraft(it) },
                onSendOtp = { viewModel.sendRegistrationOtp(it) },
                onVerifyOtp = { m, o -> viewModel.verifyRegistrationOtp(m, o) },
                onNextStep = { viewModel.nextRegistrationStep() },
                onPrevStep = { viewModel.previousRegistrationStep() },
                onSubmitRegistration = { viewModel.submitRegistration() },
                onCancelToLogin = {
                    isRegisteringView = false
                    viewModel.resetRegistration()
                }
            )
        } else {
            LoginScreen(
                onLoginSubmit = { m, o -> viewModel.loginWithOtp(m, o) },
                onNavigateToRegister = {
                    viewModel.resetRegistration()
                    isRegisteringView = true
                },
                onNavigateToAdmin = { showAdminPortal = true },
                isProcessing = isProcessingAction
            )
        }
    } else {
        // AUTHENTICATED PARTNER ROUTING ACCORDING TO VERIFICATION STATUS
        val account = currentAccount!!
        when (account.status) {
            PartnerAccountStatus.PENDING_VERIFICATION, PartnerAccountStatus.DRAFT -> {
                VerificationPendingScreen(
                    account = account,
                    onRefreshStatus = { viewModel.refreshAccountStatus() },
                    onLogout = { viewModel.logout() },
                    onOpenAdminPortal = { showAdminPortal = true }
                )
            }
            PartnerAccountStatus.ACTION_REQUIRED -> {
                ActionRequiredScreen(
                    account = account,
                    onRefreshStatus = { viewModel.refreshAccountStatus() },
                    onLogout = { viewModel.logout() },
                    onOpenAdminPortal = { showAdminPortal = true }
                )
            }
            PartnerAccountStatus.REJECTED -> {
                RejectedApplicationScreen(
                    account = account,
                    onReapply = {
                        viewModel.logout()
                        isRegisteringView = true
                    },
                    onLogout = { viewModel.logout() },
                    onOpenAdminPortal = { showAdminPortal = true }
                )
            }
            PartnerAccountStatus.SUSPENDED -> {
                SuspendedScreen(
                    account = account,
                    onLogout = { viewModel.logout() },
                    onOpenAdminPortal = { showAdminPortal = true }
                )
            }
            PartnerAccountStatus.APPROVED -> {
                // APPROVED PARTNER DASHBOARD (Full Work & Order Reception)
                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("bottom_nav_bar")
                        ) {
                            // Tab 1: Incoming / Home
                            NavigationBarItem(
                                selected = selectedTab == HomeHelpTab.INCOMING_OR_HOME,
                                onClick = { selectedTab = HomeHelpTab.INCOMING_OR_HOME },
                                icon = {
                                    if (incomingOrder != null && incomingOrder?.status == OrderStatus.OFFERED) {
                                        BadgedBox(badge = {
                                            Badge(containerColor = StatusCrimson) { Text("1", color = White) }
                                        }) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = "New Order Offer", tint = StatusCrimson)
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selectedTab == HomeHelpTab.INCOMING_OR_HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = "Home"
                                        )
                                    }
                                },
                                label = { Text("Requests") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HomeHelpNavy,
                                    indicatorColor = HomeHelpGold.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.testTag("nav_requests")
                            )

                            // Tab 2: Active Job (if accepted)
                            if (activeJob != null) {
                                NavigationBarItem(
                                    selected = selectedTab == HomeHelpTab.ACTIVE_JOB,
                                    onClick = { selectedTab = HomeHelpTab.ACTIVE_JOB },
                                    icon = {
                                        BadgedBox(badge = {
                                            Badge(containerColor = StatusEmerald) { Text("●", color = White) }
                                        }) {
                                            Icon(Icons.Filled.DirectionsRun, contentDescription = "Active Task", tint = StatusEmerald)
                                        }
                                    },
                                    label = { Text("Active Job") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = StatusEmerald,
                                        indicatorColor = StatusEmerald.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_active_job")
                                )
                            }

                            // Tab 3: History
                            NavigationBarItem(
                                selected = selectedTab == HomeHelpTab.JOBS,
                                onClick = { selectedTab = HomeHelpTab.JOBS },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == HomeHelpTab.JOBS) Icons.Filled.Assignment else Icons.Outlined.Assignment,
                                        contentDescription = "Jobs History"
                                    )
                                },
                                label = { Text("Jobs") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HomeHelpNavy,
                                    indicatorColor = HomeHelpGold.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.testTag("nav_jobs")
                            )

                            // Tab 4: Earnings
                            NavigationBarItem(
                                selected = selectedTab == HomeHelpTab.EARNINGS,
                                onClick = { selectedTab = HomeHelpTab.EARNINGS },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == HomeHelpTab.EARNINGS) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = "Earnings"
                                    )
                                },
                                label = { Text("Earnings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HomeHelpNavy,
                                    indicatorColor = HomeHelpGold.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.testTag("nav_earnings")
                            )

                            // Tab 5: Profile
                            NavigationBarItem(
                                selected = selectedTab == HomeHelpTab.PROFILE,
                                onClick = { selectedTab = HomeHelpTab.PROFILE },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == HomeHelpTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Profile"
                                    )
                                },
                                label = { Text("Profile") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = HomeHelpNavy,
                                    indicatorColor = HomeHelpGold.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.testTag("nav_profile")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            HomeHelpTab.INCOMING_OR_HOME -> {
                                val order = incomingOrder
                                if (order != null && order.status == OrderStatus.OFFERED) {
                                    IncomingOrderScreen(
                                        order = order,
                                        timeRemainingSeconds = timeRemaining,
                                        isAlarmSounding = isAlarmSounding,
                                        isProcessingAction = isProcessingAction,
                                        voicePlayerState = voicePlayerState,
                                        hasLocationPermission = hasLocationPermission,
                                        onStopAlarmSound = { viewModel.stopAlarmSound() },
                                        onToggleVoicePlayback = { url, dur -> viewModel.toggleVoicePlayback(url, dur) },
                                        onAcceptOrder = { id -> viewModel.acceptOrder(id) },
                                        onRejectOrder = { id, reason -> viewModel.rejectOrder(id, reason) },
                                        onRequestLocationPermission = {
                                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        }
                                    )
                                } else {
                                    PartnerStandbyDashboard(
                                        profile = profile,
                                        onOpenSimulator = { showSimulatorDialog = true },
                                        onToggleOnline = { viewModel.toggleOnlineStatus(it) }
                                    )
                                }
                            }

                            HomeHelpTab.ACTIVE_JOB -> {
                                val active = activeJob
                                if (active != null) {
                                    ActiveJobScreen(
                                        order = active,
                                        onUpdateStatus = { viewModel.updateOrderStatus(it) },
                                        onAddExtraCharge = { amt, reason -> viewModel.addAdditionalCharge(amt, reason) }
                                    )
                                } else {
                                    selectedTab = HomeHelpTab.INCOMING_OR_HOME
                                }
                            }

                            HomeHelpTab.JOBS -> {
                                JobsScreen(orders = allOrders)
                            }

                            HomeHelpTab.EARNINGS -> {
                                EarningsScreen(profile = profile)
                            }

                            HomeHelpTab.PROFILE -> {
                                ProfileScreen(
                                    profile = profile,
                                    onToggleOnline = { viewModel.toggleOnlineStatus(it) },
                                    onOpenSimulator = { showSimulatorDialog = true },
                                    onLogout = { viewModel.logout() },
                                    onOpenAdminPortal = { showAdminPortal = true }
                                )
                            }
                        }
                    }
                }

                if (showSimulatorDialog) {
                    SimulateOrderDialog(
                        currentOrderId = incomingOrder?.id,
                        onDismiss = { showSimulatorDialog = false },
                        onSimulateOrder = { name, desc, voice, hours ->
                            viewModel.simulateNewOrder(name, desc, voice, hours)
                            selectedTab = HomeHelpTab.INCOMING_OR_HOME
                        },
                        onSimulateCompetingClaim = { id ->
                            viewModel.simulateCompetingClaim(id)
                        }
                    )
                }
            }
        }
    }
}
