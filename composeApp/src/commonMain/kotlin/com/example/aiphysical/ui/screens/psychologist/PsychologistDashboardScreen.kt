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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiphysical.presentation.psychologist.*
import com.example.aiphysical.ui.theme.*
import com.example.aiphysical.util.BackPressHandler
import com.example.aiphysical.util.createFirestoreService
import kotlinx.coroutines.flow.collectLatest

// ─── Entry Point ──────────────────────────────────────────────────────────────

@Composable
fun PsychologistDashboardScreen(
    uid: String,
    orgId: String,
    fullName: String,
    onLogout: () -> Unit,
) {
    val vm: PsychologistViewModel = viewModel(
        factory = PsychologistViewModel.factory(
            orgId = orgId,
            uid = uid,
            psychologistName = fullName,
            firestoreService = createFirestoreService()
        )
    )

    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    // Collect MVI side-effects
    LaunchedEffect(Unit) {
        vm.effects.collectLatest { effect ->
            when (effect) {
                is PsychologistEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                PsychologistEffect.TriggerHaptic ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // Back press: in StudentDetail go back to dashboard
    BackPressHandler(
        enabled = state.currentScreen == PsychologistScreen.StudentDetail,
        onBack = { vm.onEvent(PsychologistEvent.BackToDashboard) }
    )

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
            // Only show bottom nav when not in StudentDetail view
            if (state.currentScreen == PsychologistScreen.Dashboard) {
                PsychBottomNavBar(
                    selectedTab = state.selectedTab,
                    criticalCount = state.criticalStudents.size,
                    pendingCount = state.pendingRecommendations.size,
                    onTabSelected = { vm.onEvent(PsychologistEvent.NavigateToTab(it)) }
                )
            }
        }
    ) { innerPadding ->
        // Pure matte background — no orbs, no glassmorphism
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PsychBackground)
        ) {
            AnimatedContent(
                targetState = state.selectedTab to state.currentScreen,
                transitionSpec = {
                    val isForward = targetState.first.ordinal >= initialState.first.ordinal
                    (slideInHorizontally(tween(280)) { if (isForward) it / 4 else -it / 4 } +
                     fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(220)) { if (isForward) -it / 4 else it / 4 } +
                     fadeOut(tween(220)))
                },
                label = "psych_content"
            ) { (tab, screen) ->
                when {
                    // StudentDetail is shown inside StudentDatabaseTab (it manages its own nav state)
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
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────

@Composable
private fun PsychBottomNavBar(
    selectedTab: PsychologistTab,
    criticalCount: Int,
    pendingCount: Int,
    onTabSelected: (PsychologistTab) -> Unit,
) {
    val navItems = listOf(
        PsychologistTab.Overview      to Triple("👥", "Студенты",  criticalCount),
        PsychologistTab.Database      to Triple("📊", "Аналитика", 0),
        PsychologistTab.Interventions to Triple("💬", "Помощь",    pendingCount),
        PsychologistTab.Library       to Triple("📚", "Профиль",   0),
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

