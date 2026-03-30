@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aiphysical.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.presentation.chat.SupportChatEvent
import com.example.aiphysical.presentation.chat.SupportChatViewModel
import com.example.aiphysical.presentation.student.*
import com.example.aiphysical.ui.components.FloatingUmiAvatarBadge
import com.example.aiphysical.ui.components.UmiAvatarBadge
import com.example.aiphysical.ui.screens.chat.DashboardDrawerSheet
import com.example.aiphysical.ui.screens.chat.DashboardMenuButton
import com.example.aiphysical.ui.screens.chat.DashboardOverlayDestination
import com.example.aiphysical.ui.screens.chat.PointsPlaceholderScreen
import com.example.aiphysical.ui.screens.chat.SupportChatScreen
import com.example.aiphysical.ui.theme.*
import com.example.aiphysical.ui.theme.getStrings
import com.example.aiphysical.util.createFirestoreService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ══════════════════════════════════════════════════════════════════════════════
//  StudentDashboardScreen — Entry Point
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun StudentDashboardScreen(
    uid: String,
    orgId: String,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
) {
    val firestoreService = remember { createFirestoreService() }
    val vm: StudentViewModel = viewModel(
        key = "student:$uid:$orgId",
        factory = StudentViewModel.factory(
            uid = uid,
            orgId = orgId,
            firestoreService = firestoreService,
            initialLanguage = currentLanguage,
        )
    )
    val supportVm: SupportChatViewModel = viewModel(
        key = "support-chat:student:$uid:$orgId",
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
    val uriHandler = LocalUriHandler.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var drawerOverlay by remember { mutableStateOf(DashboardOverlayDestination.None) }
    val activeTestState = state.activeTestState
    val activeCustomTestState = state.activeCustomTestState

    // Collect side-effects
    LaunchedEffect(Unit) {
        vm.effects.collectLatest { effect ->
            when (effect) {
                is StudentEffect.ShowSnackbar     -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                is StudentEffect.NavigateToTest   -> { /* Student tests open as in-screen fullscreen overlay */ }
                is StudentEffect.OpenUrl          -> {
                    try { uriHandler.openUri(effect.url) }
                    catch (_: Exception) {
                        snackbarHostState.showSnackbar(
                            when (state.currentLanguage) {
                                AppLanguage.RU -> "Не удалось открыть ссылку"
                                AppLanguage.EN -> "Failed to open the link"
                                AppLanguage.KZ -> "Сілтемені ашу мүмкін болмады"
                            }
                        )
                    }
                }
            }
        }
    }

    // One-way sync: push parent language changes down to child VMs.
    LaunchedEffect(currentLanguage) {
        vm.onEvent(StudentEvent.ChangeLanguage(currentLanguage))
        supportVm.onEvent(SupportChatEvent.ChangeLanguage(currentLanguage))
    }

    LaunchedEffect(drawerOverlay) {
        supportVm.setActive(drawerOverlay == DashboardOverlayDestination.Chat)
    }

    // Propagate user-initiated language changes (from ProfileTab LanguageSwitcher) back to Auth.
    // Guard avoids the initial oscillation: VM started with currentLanguage so on first
    // composition state.currentLanguage == latestParentLanguage → no spurious callback.
    val latestParentLanguage by rememberUpdatedState(currentLanguage)
    LaunchedEffect(state.currentLanguage) {
        if (state.currentLanguage != latestParentLanguage) {
            onLanguageChange(state.currentLanguage)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DashboardDrawerSheet(
                language = state.currentLanguage,
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
                        containerColor = Color(0xFF161632),
                        contentColor = PsychTeal,
                        modifier = Modifier.padding(16.dp).border(1.dp, PsychTeal.copy(0.3f), RoundedCornerShape(14.dp))
                    )
                }
            },
            bottomBar = {
                if (!state.showAiChat && activeTestState == null && activeCustomTestState == null) {
                    StudentBottomNavBar(
                        selectedTab = state.selectedTab,
                        strings = getStrings(state.currentLanguage),
                        onTabSelected = {
                            drawerOverlay = DashboardOverlayDestination.None
                            vm.onEvent(StudentEvent.NavigateToTab(it))
                        }
                    )
                }
            }
        ) { innerPadding ->

            // Animated deep dark background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF050010), Color(0xFF0B0B1E), Color(0xFF050010))
                        )
                    )
            ) {
                // Ambient orb — top left
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .offset(x = (-80).dp, y = (-60).dp)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF8A2BE2).copy(0.18f), Color.Transparent)),
                            CircleShape
                        )
                )
                // Ambient orb — bottom right
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 60.dp, y = 60.dp)
                        .background(
                            Brush.radialGradient(listOf(PsychTeal.copy(0.12f), Color.Transparent)),
                            CircleShape
                        )
                )

                // Main content — lightweight crossfade to avoid heavy tab re-measurements
                Crossfade(
                    targetState = state.selectedTab,
                    animationSpec = tween(140),
                    label = "student_tab"
                ) { tab ->
                    when (tab) {
                        StudentTab.Home    -> StudentHomeTab(    state = state, vm = vm, onLogout = onLogout, modifier = Modifier.padding(innerPadding))
                        StudentTab.Help    -> StudentHelpTab(
                            state = state,
                            onOpenPsychologistChat = { drawerOverlay = DashboardOverlayDestination.Chat },
                            modifier = Modifier.padding(innerPadding)
                        )
                        StudentTab.Courses -> StudentCoursesTab( state = state, vm = vm,  modifier = Modifier.padding(innerPadding))
                        StudentTab.Profile -> StudentProfileTab(
                            state = state,
                            onLogout = onLogout,
                            onLanguageChange = {
                                vm.onEvent(StudentEvent.ChangeLanguage(it))
                                onLanguageChange(it)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                // ── Text Course Viewer overlay ─────────────────────────────────────
                if (state.showTextCourseViewer && state.selectedAddedCourse != null) {
                    TextCourseViewerDialog(
                        course = state.selectedAddedCourse!!,
                        language = state.currentLanguage,
                        onDismiss = { vm.onEvent(StudentEvent.CloseTextCourse) }
                    )
                }

                // ── Student Test full-screen overlay ──────────────────────────────
                if (activeTestState != null) {
                    StudentTestScreen(
                        testState = activeTestState,
                        language = state.currentLanguage,
                        vm = vm
                    )
                }

                if (activeCustomTestState != null) {
                    StudentOrganizationCustomTestScreen(
                        session = activeCustomTestState,
                        language = state.currentLanguage,
                        onClose = { vm.onEvent(StudentEvent.CloseOrganizationCustomTest) },
                        onOptionSelected = { vm.onEvent(StudentEvent.AnswerOrganizationCustomTestQuestion(it)) },
                        onNext = { vm.onEvent(StudentEvent.NextOrganizationCustomTestQuestion) },
                        onSubmit = { vm.onEvent(StudentEvent.SubmitOrganizationCustomTest) }
                    )
                }

                // ── Floating AI Chat Button ────────────────────────────────────────
                if (activeTestState == null && activeCustomTestState == null && drawerOverlay == DashboardOverlayDestination.None) {
                    AiChatFab(
                        currentLanguage = state.currentLanguage,
                        hasMessages = state.chatMessages.isNotEmpty(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = 18.dp,
                                bottom = innerPadding.calculateBottomPadding() + 14.dp
                            )
                    ) { vm.onEvent(StudentEvent.OpenAiChat) }
                }

                // ── AI Chat Overlay ───────────────────────────────────────────────
                AnimatedVisibility(
                    visible = state.showAiChat,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(tween(180)),
                    exit  = fadeOut(tween(140))
                ) {
                    AiChatOverlay(
                        state = state,
                        vm = vm,
                        onDismiss = { vm.onEvent(StudentEvent.CloseAiChat) }
                    )
                }

                if (!state.showAiChat && activeTestState == null && activeCustomTestState == null && drawerOverlay == DashboardOverlayDestination.None) {
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
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                    DashboardOverlayDestination.Points -> PointsPlaceholderScreen(
                        title = state.currentLanguage.pick(
                            ru = "Баллы студента",
                            en = "Student points",
                            kz = "Студент ұпайлары"
                        ),
                        language = state.currentLanguage,
                        currentUserName = state.profile.fullName.ifBlank { state.profile.email },
                        currentPoints = state.profile.pointsTotal,
                        pointsHistory = state.pointsHistory,
                        introText = state.currentLanguage.pick(
                            ru = "Баллы начисляются после полного завершения тестов в интерфейсе студента.",
                            en = "Points are awarded after fully completing tests in the student interface.",
                            kz = "Ұпайлар студент интерфейсіндегі тесттер толық аяқталғаннан кейін беріледі."
                        ),
                        onBack = { drawerOverlay = DashboardOverlayDestination.None },
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                    DashboardOverlayDestination.None -> Unit
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Bottom Navigation Bar
// ══════════════════════════════════════════════════════════════════════════════

private data class NavItem(val tab: StudentTab, val emoji: String, val label: String)

@Composable
private fun StudentBottomNavBar(
    selectedTab: StudentTab,
    strings: Strings,
    onTabSelected: (StudentTab) -> Unit,
) {
    val navItems = listOf(
        NavItem(StudentTab.Home,    "🏠", strings.tabHome),
        NavItem(StudentTab.Help,    "🆘", strings.tabHelp),
        NavItem(StudentTab.Courses, "📚", strings.tabCourses),
        NavItem(StudentTab.Profile, "👤", strings.tabProfile),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Top gradient separator line
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF9D5FF5).copy(0.35f),
                            PsychTeal.copy(0.25f),
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
                        Color(0xFF050010).copy(0f),
                        Color(0xFF050010).copy(0.95f),
                        Color(0xFF050010)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                StudentNavItem(
                    item = item,
                    isSelected = selectedTab == item.tab,
                    onClick = { onTabSelected(item.tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StudentNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (item.tab == StudentTab.Help) PsychCritical else PsychTeal
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(250),
        label = "student_nav_${item.label}"
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
        // Active indicator line at top
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(indicatorAlpha), accentColor.copy(indicatorAlpha * 0.3f))
                    ),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.height(6.dp))

        // Icon pill
        Box(
            modifier = Modifier
                .background(
                    if (isSelected) accentColor.copy(0.14f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (isSelected) 1.dp else 0.dp,
                    color = if (isSelected) accentColor.copy(0.35f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 9.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(item.emoji, fontSize = if (isSelected) 20.sp else 18.sp)
        }

        Spacer(Modifier.height(3.dp))
        Text(
            item.label,
            color = if (isSelected) accentColor else TextHint,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Text Course Viewer Dialog (shared by Student, Director)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
internal fun TextCourseViewerDialog(
    course: OrganizationCourse,
    language: AppLanguage = AppLanguage.RU,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050010).copy(0.88f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFF0D0D22))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PsychTeal.copy(0.4f), Color(0xFF9D5FF5).copy(0.3f))),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drag handle
                Box(
                    Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(Color.White.copy(0.15f), RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                // Badge + title
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .background(PsychTeal.copy(0.18f), RoundedCornerShape(8.dp))
                            .border(1.dp, PsychTeal.copy(0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(language.pick("📝 Текстовый курс", "📝 Text course", "📝 Мәтіндік курс"), color = PsychTeal, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                }

                Text(
                    course.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 28.sp
                )

                if (course.description.isNotBlank()) {
                    Text(
                        course.description,
                        color = Color.White.copy(0.55f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                // Author row
                if (course.createdByName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("👤", fontSize = 14.sp)
                        Text(
                            language.pick(
                                "Автор: ${course.createdByName}",
                                "Author: ${course.createdByName}",
                                "Авторы: ${course.createdByName}"
                            ),
                            color = Color.White.copy(0.45f),
                            fontSize = 12.sp
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(0.08f))

                // Content text
                if (course.contentText.isNotBlank()) {
                    Text(
                        course.contentText,
                        color = Color.White.copy(0.85f),
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.05f))
                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(language.pick("Текст курса отсутствует", "Course text is missing", "Курс мәтіні жоқ"), color = Color.White.copy(0.35f), fontSize = 14.sp)
                    }
                }

                // Close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(PsychTeal.copy(0.25f), Color(0xFF9D5FF5).copy(0.2f))))
                        .border(1.dp, Brush.horizontalGradient(listOf(PsychTeal.copy(0.6f), Color(0xFF9D5FF5).copy(0.5f))), RoundedCornerShape(14.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(language.pick("Закрыть", "Close", "Жабу"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  AI Chat FAB — floating button bottom-right
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiChatFab(
    currentLanguage: AppLanguage,
    hasMessages: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow ring
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF9D5FF5).copy(alpha = 0.22f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // Main button
        Box(
            modifier = Modifier
                .size(56.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            UmiAvatarBadge(
                modifier = Modifier.fillMaxSize(),
                backgroundBrush = Brush.linearGradient(listOf(Color(0xFF9D5FF5), PsychTeal)),
                imagePadding = 0.dp,
                contentDescription = currentLanguage.pick("Открыть чат с Уми", "Open chat with Umi", "Умимен чатты ашу")
            )
        }

        // Notification dot (if has messages)
        if (hasMessages) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
                    .background(Color(0xFFFF4757), CircleShape)
                    .border(2.dp, Color(0xFF050010), CircleShape)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  AI Chat Overlay — bottom-sheet style panel
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiChatOverlay(
    state: StudentUiState,
    vm: StudentViewModel,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    val pulseAlpha = 0.62f

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF060616), Color(0xFF0C0C20), Color(0xFF080818))
                )
            )
    ) {
        // ── Status bar space ──────────────────────────────────────────────────
        Spacer(Modifier.statusBarsPadding())

        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF9D5FF5).copy(0.14f), Color.Transparent)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pulsing avatar
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF9D5FF5).copy(pulseAlpha * 0.5f),
                                        PsychTeal.copy(pulseAlpha * 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                    UmiAvatarBadge(
                        modifier = Modifier.size(42.dp),
                        backgroundBrush = Brush.radialGradient(
                            listOf(Color(0xFF9D5FF5).copy(0.4f), PsychTeal.copy(0.25f))
                        ),
                        borderBrush = Brush.linearGradient(listOf(Color(0xFF9D5FF5), PsychTeal)),
                        borderWidth = 1.5.dp,
                        imagePadding = 0.dp,
                        contentDescription = when (state.currentLanguage) {
                            AppLanguage.RU -> "Аватар Уми"
                            AppLanguage.EN -> "Umi avatar"
                            AppLanguage.KZ -> "Уми аватары"
                        }
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        when (state.currentLanguage) {
                            AppLanguage.RU -> "Уми"
                            AppLanguage.EN -> "Umi"
                            AppLanguage.KZ -> "Уми"
                        },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFF00E676).copy(pulseAlpha), CircleShape)
                        )
                        Text(
                            when (state.currentLanguage) {
                                AppLanguage.RU -> "Ваш помощник • ${state.chatMessages.size} сообщ."
                                AppLanguage.EN -> "Your assistant • ${state.chatMessages.size} msgs"
                                AppLanguage.KZ -> "Көмекшіңіз • ${state.chatMessages.size} хабарлама"
                            },
                            color = PsychTeal.copy(0.85f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Clear history button
                if (state.chatMessages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.07f))
                            .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { vm.onEvent(StudentEvent.ClearChatHistory) }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            when (state.currentLanguage) {
                                AppLanguage.RU -> "🗑 Очистить"
                                AppLanguage.EN -> "🗑 Clear"
                                AppLanguage.KZ -> "🗑 Тазарту"
                            },
                            color = Color.White.copy(0.55f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Close button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.08f))
                        .border(1.dp, Color.White.copy(0.15f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White.copy(0.75f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Gradient divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF9D5FF5).copy(0.6f), PsychTeal.copy(0.4f), Color.Transparent)
                    )
                )
        )

        // ── Messages area ─────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            // Ambient orb — top-left
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = (-70).dp, y = (-50).dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF9D5FF5).copy(0.07f), Color.Transparent)),
                        CircleShape
                    )
            )
            // Ambient orb — bottom-right
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 50.dp, y = 40.dp)
                    .background(
                        Brush.radialGradient(listOf(PsychTeal.copy(0.06f), Color.Transparent)),
                        CircleShape
                    )
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.chatMessages.isEmpty()) {
                    item {
                        AiChatOverlayEmptyState(
                            currentLanguage = state.currentLanguage,
                            onSuggestionClick = { suggestion ->
                                if (!state.isChatLoading) {
                                    vm.onEvent(StudentEvent.SendChatMessage(suggestion))
                                }
                            }
                        )
                    }
                }
                itemsIndexed(
                    items = state.chatMessages,
                    key = { index, msg -> "${index}_${msg.role}_${msg.isError}_${msg.text.hashCode()}" }
                ) { _, msg ->
                    ChatBubble(message = msg, language = state.currentLanguage)
                }
                if (state.isChatLoading) {
                    item { TypingIndicatorOverlay(currentLanguage = state.currentLanguage) }
                }
            }
        }

        // ── Error banner ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.chatError != null,
            enter = slideInVertically() + fadeIn(),
            exit  = slideOutVertically() + fadeOut()
        ) {
            state.chatError?.let { err ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF5370).copy(0.12f))
                        .border(1.dp, Color(0xFFFF5370).copy(0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠️", fontSize = 14.sp)
                    Text(err, color = Color(0xFFFF5370), fontSize = 12.sp, modifier = Modifier.weight(1f), lineHeight = 16.sp)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFFFF5370).copy(0.2f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { vm.onEvent(StudentEvent.ClearChatError) }
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text("✕", color = Color(0xFFFF5370), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        AiChatInputBar(
            currentLanguage = state.currentLanguage,
            value         = state.chatInput,
            isLoading     = state.isChatLoading,
            onValueChange = { vm.onEvent(StudentEvent.UpdateChatInput(it)) },
            onSend = {
                if (state.chatInput.isNotBlank() && !state.isChatLoading) {
                    vm.onEvent(StudentEvent.SendChatMessage(state.chatInput))
                }
            }
        )
    }
}

@Composable
private fun AiChatOverlayEmptyState(
    currentLanguage: AppLanguage,
    onSuggestionClick: (String) -> Unit,
) {
    val pulseAlpha = 0.42f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pulsing avatar
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(112.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF9D5FF5).copy(pulseAlpha * 0.5f),
                                PsychTeal.copy(pulseAlpha * 0.3f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            FloatingUmiAvatarBadge(
                modifier = Modifier.size(88.dp),
                levitationAmplitude = 9.dp,
                durationMillis = 2800,
                backgroundBrush = Brush.radialGradient(
                    listOf(Color(0xFF0F1027), Color(0xFF070712))
                ),
                borderBrush = Brush.sweepGradient(listOf(Color(0xFF9D5FF5), PsychTeal, Color(0xFF9D5FF5))),
                borderWidth = 2.5.dp,
                imagePadding = 0.dp,
                contentDescription = when (currentLanguage) {
                    AppLanguage.RU -> "Уми"
                    AppLanguage.EN -> "Umi"
                    AppLanguage.KZ -> "Уми"
                }
            )
        }

        Text(
            when (currentLanguage) {
                AppLanguage.RU -> "Уми готова помочь"
                AppLanguage.EN -> "Umi is ready to help"
                AppLanguage.KZ -> "Уми көмектесуге дайын"
            },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            when (currentLanguage) {
                AppLanguage.RU -> "Задайте любой вопрос о здоровье,\nучёбе или психологическом благополучии"
                AppLanguage.EN -> "Ask anything about health,\nstudy, or mental well-being"
                AppLanguage.KZ -> "Денсаулық,\nоқу немесе психологиялық әл-ауқат туралы кез келген сұрақты қойыңыз"
            },
            color = Color.White.copy(0.45f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(4.dp))

        // Suggestion chips
        val suggestions = when (currentLanguage) {
            AppLanguage.RU -> listOf(
                "💡 Как снизить стресс?",
                "📚 Советы по учёбе",
                "😴 Улучшить сон",
                "🧘 Как расслабиться?"
            )
            AppLanguage.EN -> listOf(
                "💡 How can I reduce stress?",
                "📚 Study tips",
                "😴 Improve sleep",
                "🧘 How to relax?"
            )
            AppLanguage.KZ -> listOf(
                "💡 Стресті қалай азайтамын?",
                "📚 Оқу бойынша кеңестер",
                "😴 Ұйқыны жақсарту",
                "🧘 Қалай босаңсуға болады?"
            )
        }
        suggestions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { hint ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF9D5FF5).copy(0.1f))
                            .border(1.dp, Color(0xFF9D5FF5).copy(0.3f), RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSuggestionClick(hint) }
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(hint, color = Color(0xFF9D5FF5).copy(0.9f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicatorOverlay(currentLanguage: AppLanguage) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_overlay")
    Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Brush.radialGradient(listOf(PsychTeal.copy(0.3f), Color(0xFF9D5FF5).copy(0.15f))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            UmiAvatarBadge(
                modifier = Modifier.fillMaxSize(),
                borderColor = PsychTeal.copy(0.4f),
                borderWidth = 1.dp,
                imagePadding = 0.dp,
                contentDescription = currentLanguage.pick("Уми печатает", "Umi is typing", "Уми теріп жатыр")
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1A1A35))
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue  = 1f,
                        animationSpec = infiniteRepeatable(
                            animation  = tween(600, delayMillis = index * 200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(dotScale)
                            .background(PsychTeal.copy(0.8f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiChatInputBar(
    currentLanguage: AppLanguage,
    value: String,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val overLimit = (value.length / 4) > 50_000

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars
                    .union(WindowInsets.ime)
                    .only(WindowInsetsSides.Bottom)
            )
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF060616).copy(0.0f), Color(0xFF060616))
                )
            )
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value         = value,
                onValueChange = onValueChange,
                modifier      = Modifier.weight(1f),
                placeholder   = {
                    Text(
                        when (currentLanguage) {
                            AppLanguage.RU -> "Задайте вопрос Уми..."
                            AppLanguage.EN -> "Ask Umi anything..."
                            AppLanguage.KZ -> "Умиге сұрақ қойыңыз..."
                        },
                        color = Color.White.copy(0.35f),
                        fontSize = 14.sp
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4,
                isError  = overLimit,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = if (overLimit) Color(0xFFFF5370) else PsychTeal.copy(0.7f),
                    unfocusedBorderColor    = Color.White.copy(0.15f),
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White,
                    cursorColor             = PsychTeal,
                    focusedContainerColor   = Color(0xFF0D0D22),
                    unfocusedContainerColor = Color(0xFF0D0D22),
                    errorBorderColor        = Color(0xFFFF5370),
                    errorContainerColor     = Color(0xFFFF5370).copy(0.05f)
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (!isLoading && value.isNotBlank() && !overLimit)
                            Brush.linearGradient(listOf(Color(0xFF9D5FF5), PsychTeal))
                        else
                            Brush.linearGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.05f)))
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isLoading && value.isNotBlank() && !overLimit,
                        onClick = onSend
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PsychTeal, strokeWidth = 2.dp)
                } else {
                    Text(
                        "➤",
                        fontSize   = 18.sp,
                        color      = if (value.isNotBlank() && !overLimit) Color.White else Color.White.copy(0.25f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
