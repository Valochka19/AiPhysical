@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiphysical.presentation.chat.SupportChatEvent
import com.example.aiphysical.presentation.chat.SupportChatViewModel
import com.example.aiphysical.presentation.psychologist.*
import com.example.aiphysical.ui.screens.chat.DashboardDrawerSheet
import com.example.aiphysical.ui.screens.chat.DashboardMenuButton
import com.example.aiphysical.ui.screens.chat.DashboardOverlayDestination
import com.example.aiphysical.ui.screens.chat.PointsPlaceholderScreen
import com.example.aiphysical.ui.screens.chat.SupportChatScreen
import com.example.aiphysical.ui.theme.*
import com.example.aiphysical.ui.theme.getStrings
import com.example.aiphysical.util.BackPressHandler
import com.example.aiphysical.util.createFirestoreService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ─── Entry Point ──────────────────────────────────────────────────────────────

@Composable
fun PsychologistDashboardScreen(
    uid: String,
    orgId: String,
    fullName: String,
    currentLanguage: com.example.aiphysical.presentation.auth.AppLanguage,
    onLanguageChange: (com.example.aiphysical.presentation.auth.AppLanguage) -> Unit,
    onLogout: () -> Unit,
) {
    val firestoreService = remember { createFirestoreService() }
    val vm: PsychologistViewModel = viewModel(
        key = "psychologist:$uid:$orgId",
        factory = PsychologistViewModel.factory(
            orgId = orgId,
            uid = uid,
            psychologistName = fullName,
            firestoreService = firestoreService,
            initialLanguage = currentLanguage,
        )
    )
    val supportVm: SupportChatViewModel = viewModel(
        key = "support-chat:psychologist:$uid:$orgId",
        factory = SupportChatViewModel.factory(
            uid = uid,
            orgId = orgId,
            firestoreService = firestoreService,
            initialLanguage = currentLanguage,
        )
    )

    val state by vm.state.collectAsStateWithLifecycle()
    val supportState by supportVm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var drawerOverlay by remember { mutableStateOf(DashboardOverlayDestination.None) }

    // Collect MVI side-effects
    LaunchedEffect(Unit) {
        vm.effects.collectLatest { effect ->
            when (effect) {
                is PsychologistEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                PsychologistEffect.TriggerHaptic ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                is PsychologistEffect.OpenUrl -> {
                    try { uriHandler.openUri(effect.url) }
                    catch (_: Exception) {
                        snackbarHostState.showSnackbar(
                            when (currentLanguage) {
                                com.example.aiphysical.presentation.auth.AppLanguage.RU -> "Не удалось открыть ссылку"
                                com.example.aiphysical.presentation.auth.AppLanguage.EN -> "Failed to open the link"
                                com.example.aiphysical.presentation.auth.AppLanguage.KZ -> "Сілтемені ашу мүмкін болмады"
                            }
                        )
                    }
                }
            }
        }
    }

    // One-way sync: when parent language changes (e.g. restored after re-login) push it down.
    LaunchedEffect(currentLanguage) {
        vm.onEvent(PsychologistEvent.ChangeLanguage(currentLanguage))
        supportVm.onEvent(SupportChatEvent.ChangeLanguage(currentLanguage))
    }

    LaunchedEffect(drawerOverlay) {
        supportVm.setActive(drawerOverlay == DashboardOverlayDestination.Chat)
    }

    // Propagate user-initiated language changes (via LanguageSwitcher in tabs) back to Auth.
    // Guard with rememberUpdatedState to avoid the initial echo that causes the oscillation loop.
    val latestParentLanguage by rememberUpdatedState(currentLanguage)
    LaunchedEffect(state.currentLanguage) {
        if (state.currentLanguage != latestParentLanguage) {
            onLanguageChange(state.currentLanguage)
        }
    }

    // Back press: close nested screens safely
    BackPressHandler(
        enabled = state.currentScreen != PsychologistScreen.Dashboard,
        onBack = {
            when (state.currentScreen) {
                PsychologistScreen.StudentDetail -> vm.onEvent(PsychologistEvent.BackToDashboard)
                PsychologistScreen.TestBuilder -> vm.onEvent(PsychologistEvent.CloseAddTestScreen)
                PsychologistScreen.Dashboard -> Unit
            }
        }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DashboardDrawerSheet(
                language = state.currentLanguage,
                onProfileClick = {
                    drawerOverlay = DashboardOverlayDestination.None
                    vm.onEvent(PsychologistEvent.NavigateToTab(PsychologistTab.Library))
                    coroutineScope.launch { drawerState.close() }
                },
                onPointsClick = {
                    drawerOverlay = DashboardOverlayDestination.Points
                    coroutineScope.launch { drawerState.close() }
                },
                onChatClick = {
                    drawerOverlay = DashboardOverlayDestination.Chat
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MatteSurface,
                        contentColor = PsychTeal,
                        actionColor = PsychTeal,
                        modifier = Modifier
                            .padding(16.dp)
                            .border(1.dp, PsychTeal.copy(0.3f), RoundedCornerShape(14.dp))
                    )
                }
            },
            bottomBar = {
                if (state.currentScreen == PsychologistScreen.Dashboard) {
                    PsychBottomNavBar(
                        selectedTab = state.selectedTab,
                        criticalCount = state.criticalStudents.size,
                        pendingCount = state.pendingRecommendations.size,
                        strings = getStrings(state.currentLanguage),
                        onTabSelected = {
                            drawerOverlay = DashboardOverlayDestination.None
                            vm.onEvent(PsychologistEvent.NavigateToTab(it))
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PsychBackground)
            ) {
                Crossfade(
                    targetState = state.selectedTab to state.currentScreen,
                    animationSpec = tween(160),
                    label = "psych_content"
                ) { (tab, screen) ->
                    when {
                        screen == PsychologistScreen.TestBuilder -> {
                            PsychologistCustomTestBuilderScreen(
                                state = state,
                                vm = vm,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        screen == PsychologistScreen.StudentDetail ||
                        tab == PsychologistTab.Database -> {
                            StudentDatabaseTab(
                                state = state,
                                vm = vm,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        tab == PsychologistTab.Overview -> {
                            PatientOverviewTab(
                                state = state,
                                vm = vm,
                                onLogout = onLogout,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        tab == PsychologistTab.Interventions -> {
                            InterventionsTab(
                                state = state,
                                vm = vm,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        tab == PsychologistTab.Library -> {
                            LibraryTab(
                                state = state,
                                vm = vm,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        else -> {
                            PatientOverviewTab(
                                state = state,
                                vm = vm,
                                onLogout = onLogout,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }

                if (state.showRecommendationSheet) {
                    RecommendationSheet(state = state, vm = vm)
                }

                val feedItem = state.selectedTestFeedItem
                if (state.showTestResultSheet && feedItem != null) {
                    TestResultReportSheet(
                        item = feedItem,
                        language = state.currentLanguage,
                        onDismiss = { vm.onEvent(PsychologistEvent.DismissTestResultSheet) }
                    )
                }

                if (drawerOverlay == DashboardOverlayDestination.None && state.currentScreen == PsychologistScreen.Dashboard) {
                    DashboardMenuButton(
                        onClick = { coroutineScope.launch { drawerState.open() } },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = 10.dp, end = 16.dp)
                    )
                }

                when (drawerOverlay) {
                    DashboardOverlayDestination.Chat -> SupportChatScreen(
                        state = supportState,
                        state.currentLanguage,
                        onBack = { drawerOverlay = DashboardOverlayDestination.None },
                        onContactSelected = { supportVm.onEvent(SupportChatEvent.SelectContact(it)) },
                        onConversationBack = { supportVm.onEvent(SupportChatEvent.ClearSelection) },
                        onInputChange = { supportVm.onEvent(SupportChatEvent.UpdateInput(it)) },
                        onSend = { supportVm.onEvent(SupportChatEvent.SendMessage) },
                        onDismissError = { supportVm.onEvent(SupportChatEvent.DismissError) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                    DashboardOverlayDestination.Points -> PointsPlaceholderScreen(
                        title = when (state.currentLanguage) {
                            com.example.aiphysical.presentation.auth.AppLanguage.RU -> "Баллы психолога"
                            com.example.aiphysical.presentation.auth.AppLanguage.EN -> "Psychologist points"
                            com.example.aiphysical.presentation.auth.AppLanguage.KZ -> "Психолог ұпайлары"
                        },
                        language = state.currentLanguage,
                        currentUserName = state.psychologistName,
                        currentPoints = state.students.sumOf { it.pointsTotal },
                        leaderboard = state.students,
                        introText = when (state.currentLanguage) {
                            com.example.aiphysical.presentation.auth.AppLanguage.RU -> "Здесь видно суммарную активность и рейтинг студентов вашей организации по заработанным баллам."
                            com.example.aiphysical.presentation.auth.AppLanguage.EN -> "Here you can see the total activity and student ranking in your organization by earned points."
                            com.example.aiphysical.presentation.auth.AppLanguage.KZ -> "Мұнда ұйымыңыздағы студенттердің жалпы белсенділігі мен жиналған ұпайлар бойынша рейтингі көрінеді."
                        },
                        onBack = { drawerOverlay = DashboardOverlayDestination.None },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                    DashboardOverlayDestination.None -> Unit
                }
            }
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────

@Composable
private fun PsychBottomNavBar(
    selectedTab: PsychologistTab,
    criticalCount: Int,
    pendingCount: Int,
    strings: Strings,
    onTabSelected: (PsychologistTab) -> Unit,
) {
    val navItems = listOf(
        PsychologistTab.Overview      to Triple("👥", strings.psychTabStudents, criticalCount),
        PsychologistTab.Database      to Triple("📊", strings.tabAnalytics,     0),
        PsychologistTab.Interventions to Triple("💬", strings.psychTabHelp,     pendingCount),
        PsychologistTab.Library       to Triple("📚", strings.tabProfile,       0),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            PsychTeal.copy(0.35f),
                            PsychCritical.copy(0.2f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        PsychBackground.copy(alpha = 0f),
                        PsychBackground.copy(alpha = 0.95f),
                        PsychBackground
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (tab, triple) ->
                val (emoji, label, badge) = triple
                PsychNavItem(
                    emoji = emoji,
                    label = label,
                    badgeCount = badge,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PsychNavItem(
    emoji: String,
    label: String,
    badgeCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(250),
        label = "nav_indicator_$label"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Top active indicator line
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(PsychTeal.copy(indicatorAlpha), PsychTeal.copy(indicatorAlpha * 0.4f))
                    ),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.height(6.dp))

        // Icon pill with optional badge
        Box {
            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) PsychTeal.copy(0.14f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) PsychTeal.copy(0.35f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = if (isSelected) 20.sp else 18.sp)
            }

            // Badge for critical/pending counts
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(PsychCritical, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (badgeCount > 9) "9+" else "$badgeCount",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (isSelected) PsychTeal else TextHint,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

