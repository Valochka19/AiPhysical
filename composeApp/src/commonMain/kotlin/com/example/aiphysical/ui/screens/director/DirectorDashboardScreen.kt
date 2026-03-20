@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aiphysical.ui.screens.director

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiphysical.presentation.director.*
import com.example.aiphysical.ui.components.*
import com.example.aiphysical.ui.theme.*
import com.example.aiphysical.util.BackPressHandler
import com.example.aiphysical.util.createFirestoreService
import kotlinx.coroutines.flow.collectLatest

// ─── Entry Point ──────────────────────────────────────────────────────────────

@Composable
fun DirectorDashboardScreen(
    orgId: String,
    uid: String,
    initialLanguage: com.example.aiphysical.presentation.auth.AppLanguage,
    onLanguageChange: (com.example.aiphysical.presentation.auth.AppLanguage) -> Unit,
    onLogout: () -> Unit,
) {
    val vm: DirectorDashboardViewModel = viewModel(
        key = "director:$uid:$orgId",
        factory = DirectorDashboardViewModel.factory(orgId, uid, createFirestoreService())
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    @Suppress("DEPRECATION") val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current

    // Sync initial language
    LaunchedEffect(initialLanguage) { vm.onEvent(DirectorEvent.ChangeLanguage(initialLanguage)) }
    LaunchedEffect(state.currentLanguage) {
        if (state.currentLanguage != initialLanguage) {
            onLanguageChange(state.currentLanguage)
        }
    }

    // Collect side effects (MVI Effects pattern)
    LaunchedEffect(Unit) {
        vm.effects.collectLatest { effect ->
            when (effect) {
                is DirectorEffect.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(effect.text))
                    snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                }
                is DirectorEffect.OpenUrl -> uriHandler.openUri(effect.url)
                is DirectorEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                DirectorEffect.TriggerHaptic -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // Back press handling
    BackPressHandler(
        enabled = state.currentScreen == DirectorPanelScreen.MemberDetail,
        onBack = { vm.onEvent(DirectorEvent.BackToDashboard) }
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CardSurfaceLight,
                    contentColor = CyanAccent,
                    actionColor = NeonViolet,
                    modifier = Modifier.padding(16.dp).border(1.dp, NeonViolet.copy(0.3f), RoundedCornerShape(14.dp))
                )
            }
        },
        bottomBar = {
            if (state.currentScreen == DirectorPanelScreen.Dashboard) {
                DirectorBottomNavBar(
                    selectedTab = state.selectedTab,
                    strings = getStrings(state.currentLanguage),
                    onTabSelected = { vm.onEvent(DirectorEvent.NavigateToTab(it)) }
                )
            }
        }
    ) { innerPadding ->
        DirectorBackground {
            AnimatedContent(
                targetState = state.currentScreen,
                transitionSpec = {
                    val forward = targetState == DirectorPanelScreen.MemberDetail
                    (fadeIn(tween(350)) + slideInHorizontally(tween(350)) { if (forward) it / 4 else -it / 4 }) togetherWith
                    (fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { if (forward) -it / 4 else it / 4 })
                },
                label = "director_screen_nav"
            ) { screen ->
                when (screen) {
                    DirectorPanelScreen.Dashboard -> {
                        AnimatedContent(
                            targetState = state.selectedTab,
                            transitionSpec = {
                                val goRight = targetState.ordinal > initialState.ordinal
                                (slideInHorizontally(tween(280)) { if (goRight) it / 3 else -it / 3 } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(200)) { if (goRight) -it / 3 else it / 3 } + fadeOut(tween(200)))
                            },
                            label = "director_tab_nav"
                        ) { tab ->
                            when (tab) {
                                DirectorTab.Dashboard   -> DashboardTab(state = state, vm = vm, onLogout = onLogout, modifier = Modifier.padding(innerPadding))
                                DirectorTab.Analytics   -> AnalyticsTab(state = state, vm = vm, modifier = Modifier.padding(innerPadding))
                                DirectorTab.Management  -> ManagementTab(state = state, vm = vm, modifier = Modifier.padding(innerPadding))
                                DirectorTab.Content     -> ContentTab(state = state, vm = vm, modifier = Modifier.padding(innerPadding))
                            }
                        }
                    }
                    DirectorPanelScreen.MemberDetail -> MemberDetailScreen(
                        state = state,
                        onEvent = vm::onEvent,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Logout relay
        if (state.currentScreen == DirectorPanelScreen.Dashboard) {
            LaunchedEffect(state.currentLanguage) { /* force recompose on language change */ }
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────

@Composable
private fun DirectorBottomNavBar(
    selectedTab: DirectorTab,
    strings: Strings,
    onTabSelected: (DirectorTab) -> Unit,
) {
    val navItems = listOf(
        DirectorTab.Dashboard  to ("🏠" to strings.tabDashboard),
        DirectorTab.Analytics  to ("📊" to strings.tabAnalytics),
        DirectorTab.Management to ("👥" to strings.tabManagement),
        DirectorTab.Content    to ("📚" to strings.tabContent),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Top gradient separator line
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, NeonViolet.copy(0.5f), CyanAccent.copy(0.4f), Color.Transparent)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        NeonBackground.copy(alpha = 0f),
                        NeonBackground.copy(alpha = 0.92f),
                        NeonBackground.copy(alpha = 0.98f)
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
            navItems.forEach { (tab, pair) ->
                val (emoji, label) = pair
                val isSelected = selectedTab == tab
                NavItem(
                    emoji = emoji, label = label, isSelected = isSelected,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgAlpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0f, animationSpec = tween(250), label = "nav_bg_$label")

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active pill indicator
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(listOf(NeonViolet.copy(bgAlpha), CyanAccent.copy(bgAlpha))),
                    androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.height(6.dp))
        // Icon inside a pill for selected state
        Box(
            modifier = Modifier
                .background(
                    if (isSelected) Brush.horizontalGradient(listOf(NeonViolet.copy(0.18f), CyanAccent.copy(0.12f)))
                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                    androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                .border(
                    if (isSelected) 1.dp else 0.dp,
                    if (isSelected) NeonViolet.copy(0.3f) else Color.Transparent,
                    androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = if (isSelected) 20.sp else 18.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (isSelected) NeonViolet else TextHint,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ─── Shared helper functions (used across tabs) ───────────────────────────────

fun statusLabel(status: String, strings: Strings, role: String = ""): String = when (status) {
    "normal"   -> strings.statusNormal
    "stress"   -> strings.statusStress
    "critical" -> strings.statusCritical
    else       -> when (role) {
        "user"         -> strings.roleStudentShort
        "teacher"      -> strings.roleTeacherShort
        "psychologist" -> strings.rolePsychShort
        "director"     -> strings.roleDirectorShort
        else           -> strings.statusUnknown
    }
}

