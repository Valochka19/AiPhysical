@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aiphysical.ui.screens.teacher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiphysical.presentation.chat.SupportChatEvent
import com.example.aiphysical.presentation.chat.SupportChatViewModel
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.data.service.FirestoreResult
import com.example.aiphysical.presentation.student.StudentEffect
import com.example.aiphysical.presentation.student.StudentEvent
import com.example.aiphysical.presentation.student.StudentTab
import com.example.aiphysical.presentation.student.StudentUiState
import com.example.aiphysical.presentation.student.StudentViewModel
import com.example.aiphysical.ui.screens.chat.DashboardDrawerSheet
import com.example.aiphysical.ui.screens.chat.DashboardMenuButton
import com.example.aiphysical.ui.screens.chat.DashboardOverlayDestination
import com.example.aiphysical.ui.screens.chat.PointsPlaceholderScreen
import com.example.aiphysical.ui.screens.chat.SupportChatScreen
import com.example.aiphysical.ui.screens.student.StudentCoursesTab
import com.example.aiphysical.ui.screens.student.TextCourseViewerDialog
import com.example.aiphysical.ui.theme.AlertOrange
import com.example.aiphysical.ui.theme.GlassBorder
import com.example.aiphysical.ui.theme.PsychTeal
import com.example.aiphysical.ui.theme.Strings
import com.example.aiphysical.ui.theme.SurfaceDeep
import com.example.aiphysical.ui.theme.TextHint
import com.example.aiphysical.ui.theme.TextPrimary
import com.example.aiphysical.ui.theme.TextSecondary
import com.example.aiphysical.ui.theme.VioletGlow
import com.example.aiphysical.ui.theme.getStrings
import com.example.aiphysical.util.createFirestoreService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun TeacherDashboardScreen(
    uid: String,
    orgId: String,
    onLogout: () -> Unit,
) {
    val firestoreService = remember { createFirestoreService() }
    val vm: StudentViewModel = viewModel(
        key = "teacher:$uid:$orgId",
        factory = StudentViewModel.factory(
            uid = uid,
            orgId = orgId,
            firestoreService = firestoreService
        )
    )
    val supportVm: SupportChatViewModel = viewModel(
        key = "support-chat:teacher:$uid:$orgId",
        factory = SupportChatViewModel.factory(
            uid = uid,
            orgId = orgId,
            firestoreService = firestoreService
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val supportState by supportVm.state.collectAsStateWithLifecycle()
    val organizationMembers by produceState(initialValue = emptyList<UserProfile>(), key1 = orgId) {
        if (orgId.isBlank()) return@produceState
        firestoreService.observeOrganizationMembers(orgId).collect { result ->
            if (result is FirestoreResult.MembersSuccess) {
                value = result.members.filter { it.role == "user" }
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var drawerOverlay by remember { mutableStateOf(DashboardOverlayDestination.None) }

    LaunchedEffect(Unit) {
        vm.effects.collectLatest { effect ->
            when (effect) {
                is StudentEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                is StudentEffect.OpenUrl -> {
                    try {
                        uriHandler.openUri(effect.url)
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar("Не удалось открыть ссылку")
                    }
                }
                is StudentEffect.NavigateToTest -> Unit
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DashboardDrawerSheet(
                onProfileClick = {
                    vm.onEvent(StudentEvent.NavigateToTab(StudentTab.Profile))
                    drawerOverlay = DashboardOverlayDestination.None
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
                        containerColor = Color(0xFF1A1428),
                        contentColor = AlertOrange,
                        modifier = Modifier
                            .padding(16.dp)
                            .border(1.dp, AlertOrange.copy(0.28f), RoundedCornerShape(14.dp))
                    )
                }
            },
            bottomBar = {
                TeacherBottomNavBar(
                    selectedTab = state.selectedTab,
                    strings = getStrings(state.currentLanguage),
                    onTabSelected = {
                        drawerOverlay = DashboardOverlayDestination.None
                        vm.onEvent(StudentEvent.NavigateToTab(it))
                    }
                )
            }
        ) { innerPadding ->
            TeacherBackground {
                AnimatedContent(
                    targetState = state.selectedTab,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "teacher_tab"
                ) { tab ->
                    when (tab) {
                        StudentTab.Home -> TeacherHomeTab(
                            state = state,
                            modifier = Modifier.padding(innerPadding),
                            onOpenCourses = { vm.onEvent(StudentEvent.NavigateToTab(StudentTab.Courses)) },
                            onOpenHelp = { vm.onEvent(StudentEvent.NavigateToTab(StudentTab.Help)) },
                            onShowTeacherTestsStub = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(getStrings(state.currentLanguage).teacherTestsSoon)
                                }
                            },
                            onLogout = onLogout
                        )
                        StudentTab.Help -> TeacherHelpTab(
                            state = state,
                            onOpenPsychologistChat = { drawerOverlay = DashboardOverlayDestination.Chat },
                            modifier = Modifier.padding(innerPadding)
                        )
                        StudentTab.Courses -> StudentCoursesTab(
                            state = state,
                            vm = vm,
                            modifier = Modifier.padding(innerPadding)
                        )
                        StudentTab.Profile -> TeacherProfileTab(
                            state = state,
                            onLogout = onLogout,
                            onLanguageChange = { vm.onEvent(StudentEvent.ChangeLanguage(it)) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                if (state.showTextCourseViewer && state.selectedAddedCourse != null) {
                    TextCourseViewerDialog(
                        course = state.selectedAddedCourse!!,
                        onDismiss = { vm.onEvent(StudentEvent.CloseTextCourse) }
                    )
                }

                if (drawerOverlay == DashboardOverlayDestination.None) {
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
                        onBack = { drawerOverlay = DashboardOverlayDestination.None },
                        onContactSelected = { supportVm.onEvent(SupportChatEvent.SelectContact(it)) },
                        onConversationBack = { supportVm.onEvent(SupportChatEvent.ClearSelection) },
                        onInputChange = { supportVm.onEvent(SupportChatEvent.UpdateInput(it)) },
                        onSend = { supportVm.onEvent(SupportChatEvent.SendMessage) },
                        onDismissError = { supportVm.onEvent(SupportChatEvent.DismissError) },
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                    DashboardOverlayDestination.Points -> PointsPlaceholderScreen(
                        title = "Баллы преподавателя",
                        currentUserName = state.profile.fullName.ifBlank { state.profile.email },
                        currentPoints = state.profile.pointsTotal,
                        leaderboard = organizationMembers,
                        introText = "Баллы студентов начисляются после полного завершения тестов. Здесь можно смотреть рейтинг вашей организации.",
                        onBack = { drawerOverlay = DashboardOverlayDestination.None },
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                    DashboardOverlayDestination.None -> Unit
                }
            }
        }
    }
}

@Composable
private fun TeacherBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF120710), Color(0xFF1B1020), Color(0xFF110713))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(listOf(AlertOrange.copy(0.18f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(listOf(PsychTeal.copy(0.12f), Color.Transparent)),
                    CircleShape
                )
        )
        content()
    }
}

@Composable
private fun TeacherHomeTab(
    state: StudentUiState,
    modifier: Modifier = Modifier,
    onOpenCourses: () -> Unit,
    onOpenHelp: () -> Unit,
    onShowTeacherTestsStub: () -> Unit,
    onLogout: () -> Unit,
) {
    val strings = getStrings(state.currentLanguage)
    val testCards = teacherTestCardTitles(state.currentLanguage)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TeacherHeroCard(
            state = state,
            strings = strings,
            onOpenHelp = onOpenHelp,
            onLogout = onLogout
        )

        TeacherQuickStats(state = state, strings = strings)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = strings.teacherTestsTitle,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = strings.teacherTestsSoon,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            testCards.forEach { (emoji, title) ->
                TeacherPlaceholderCard(
                    emoji = emoji,
                    title = title,
                    subtitle = strings.teacherTestsSoon,
                    onClick = onShowTeacherTestsStub
                )
            }
        }

        TeacherActionCard(
            emoji = "📚",
            title = strings.tabCourses,
            subtitle = strings.teacherDashboardSubtitle,
            accent = PsychTeal,
            onClick = onOpenCourses
        )

        TeacherActionCard(
            emoji = "🧠",
            title = strings.teacherHelpTitle,
            subtitle = strings.teacherHelpText,
            accent = AlertOrange,
            onClick = onOpenHelp
        )
    }
}

@Composable
private fun TeacherHeroCard(
    state: StudentUiState,
    strings: Strings,
    onOpenHelp: () -> Unit,
    onLogout: () -> Unit,
) {
    val firstName = state.profile.fullName.split(" ").firstOrNull().orEmpty().ifBlank { strings.roleTeacherShort }
    val statusColor = when (state.profile.latestAiStatus) {
        "critical" -> Color(0xFFFF6B6B)
        "stress" -> AlertOrange
        "normal" -> PsychTeal
        else -> VioletGlow
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(AlertOrange.copy(0.16f), Color.White.copy(0.04f))))
            .border(1.dp, Brush.horizontalGradient(listOf(AlertOrange.copy(0.55f), PsychTeal.copy(0.30f))), RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Brush.radialGradient(listOf(AlertOrange.copy(0.45f), Color.Transparent)), CircleShape)
                        .border(1.dp, AlertOrange.copy(0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑‍🏫", fontSize = 24.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .background(AlertOrange.copy(0.16f), RoundedCornerShape(999.dp))
                            .border(1.dp, AlertOrange.copy(0.35f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(strings.roleTeacherShort, color = AlertOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(strings.teacherDashboardTitle, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("$firstName, ${strings.teacherDashboardSubtitle}", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(0.06f))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(12.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onLogout)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(strings.logoutBtn, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(statusColor.copy(0.15f), RoundedCornerShape(999.dp))
                    .border(1.dp, statusColor.copy(0.4f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = when (state.profile.latestAiStatus) {
                        "critical" -> strings.statusCriticalFull
                        "stress" -> strings.statusStressFull
                        "normal" -> strings.statusNormalFull
                        else -> strings.statusNoData
                    },
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = strings.teacherHelpText,
                color = TextHint,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "→",
                color = AlertOrange,
                fontSize = 18.sp,
                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenHelp)
            )
        }
    }
}

@Composable
private fun TeacherQuickStats(state: StudentUiState, strings: Strings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TeacherMiniStat(
            label = strings.tabCourses,
            value = "${state.addedCourses.size}",
            accent = PsychTeal,
            modifier = Modifier.weight(1f)
        )
        TeacherMiniStat(
            label = strings.profileCourseProgress,
            value = "${state.profile.courseProgressPercent.toInt()}%",
            accent = AlertOrange,
            modifier = Modifier.weight(1f)
        )
        TeacherMiniStat(
            label = strings.profileRole,
            value = strings.roleTeacherShort,
            accent = VioletGlow,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TeacherMiniStat(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(0.10f))
            .border(1.dp, accent.copy(0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(label, color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TeacherPlaceholderCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(AlertOrange.copy(0.12f), Color.White.copy(0.04f))))
            .border(1.dp, AlertOrange.copy(0.24f), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(AlertOrange.copy(0.14f), RoundedCornerShape(14.dp))
                .border(1.dp, AlertOrange.copy(0.32f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Text("›", color = AlertOrange, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TeacherActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(0.12f), Color.White.copy(0.03f))))
            .border(1.dp, accent.copy(0.26f), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Text("→", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TeacherHelpTab(
    state: StudentUiState,
    onOpenPsychologistChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 28.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(strings.teacherHelpTitle, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(strings.teacherHelpText, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)

        TeacherActionCard(
            emoji = "🧠",
            title = strings.contactPsychologist,
            subtitle = if (state.profile.psychComment.isNotBlank()) state.profile.psychComment else strings.teacherHelpText,
            accent = PsychTeal,
            onClick = onOpenPsychologistChat
        )

        TeacherActionCard(
            emoji = "📞",
            title = "150",
            subtitle = when (state.currentLanguage) {
                AppLanguage.RU -> "Экстренная психологическая помощь"
                AppLanguage.EN -> "Emergency psychological support"
                AppLanguage.KZ -> "Шұғыл психологиялық көмек"
            },
            accent = AlertOrange,
            onClick = {}
        )
    }
}

@Composable
private fun TeacherProfileTab(
    state: StudentUiState,
    onLogout: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 28.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(Brush.radialGradient(listOf(AlertOrange.copy(0.5f), AlertOrange.copy(0.12f))), CircleShape)
                    .border(2.dp, AlertOrange.copy(0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🧑‍🏫", fontSize = 34.sp)
            }
            Text(state.profile.fullName, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(state.profile.email, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.language, color = TextHint, fontSize = 11.sp, modifier = Modifier.weight(1f))
            listOf(AppLanguage.RU, AppLanguage.EN, AppLanguage.KZ).forEach { lang ->
                val isSelected = state.currentLanguage == lang
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AlertOrange.copy(0.20f) else Color.White.copy(0.05f))
                        .border(1.dp, if (isSelected) AlertOrange.copy(0.60f) else Color.White.copy(0.12f), RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onLanguageChange(lang) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(lang.code.uppercase(), color = if (isSelected) AlertOrange else TextHint, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.03f))))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TeacherProfileRow("🎭", strings.profileRole, strings.roleTeacherShort)
            TeacherProfileRow("🏫", strings.profileGroup, strings.filterStaff)
            TeacherProfileRow("📚", strings.profileCourseProgress, "${state.profile.courseProgressPercent.toInt()}%")
            TeacherProfileRow("🧠", strings.teacherHelpTitle, if (state.profile.psychComment.isNotBlank()) strings.statusNormal else strings.statusNoData)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AlertOrange.copy(0.12f))
                .border(1.dp, AlertOrange.copy(0.35f), RoundedCornerShape(16.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onLogout)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🚪 ${strings.profileLogout}", color = AlertOrange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TeacherProfileRow(emoji: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Text(label, color = TextSecondary, fontSize = 14.sp)
        }
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun TeacherBottomNavBar(
    selectedTab: StudentTab,
    strings: Strings,
    onTabSelected: (StudentTab) -> Unit,
) {
    val navItems = listOf(
        StudentTab.Home to ("🧑‍🏫" to strings.tabHome),
        StudentTab.Help to ("🧠" to strings.tabHelp),
        StudentTab.Courses to ("📚" to strings.tabCourses),
        StudentTab.Profile to ("👤" to strings.tabProfile),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, SurfaceDeep.copy(alpha = 0.92f), SurfaceDeep)
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
            navItems.forEach { (tab, item) ->
                TeacherNavItem(
                    emoji = item.first,
                    label = item.second,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TeacherNavItem(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(220),
        label = "teacher_nav_$label"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(listOf(AlertOrange.copy(indicatorAlpha), PsychTeal.copy(indicatorAlpha * 0.75f))),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .background(if (isSelected) AlertOrange.copy(0.12f) else Color.Transparent, RoundedCornerShape(12.dp))
                .border(if (isSelected) 1.dp else 0.dp, if (isSelected) AlertOrange.copy(0.30f) else Color.Transparent, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = if (isSelected) 20.sp else 18.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (isSelected) AlertOrange else TextHint,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun teacherTestCardTitles(language: AppLanguage): List<Pair<String, String>> = when (language) {
    AppLanguage.RU -> listOf(
        "🪫" to "Проверка нагрузки",
        "🌤" to "Эмоциональный фон",
        "🤝" to "Климат в классе"
    )
    AppLanguage.EN -> listOf(
        "🪫" to "Workload check",
        "🌤" to "Emotional state",
        "🤝" to "Class climate"
    )
    AppLanguage.KZ -> listOf(
        "🪫" to "Жүктеме тексеруі",
        "🌤" to "Эмоциялық ахуал",
        "🤝" to "Сынып ахуалы"
    )
}

