@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aiphysical.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.presentation.student.*
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
    onLogout: () -> Unit,
) {
    val vm: StudentViewModel = viewModel(
        factory = StudentViewModel.factory(
            uid = uid,
            orgId = orgId,
            firestoreService = createFirestoreService()
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    // Collect side-effects
    LaunchedEffect(Unit) {
        vm.effects.collectLatest { effect ->
            when (effect) {
                is StudentEffect.ShowSnackbar     -> snackbarHostState.showSnackbar(effect.message, duration = SnackbarDuration.Short)
                is StudentEffect.NavigateToTest   -> { /* Student tests open as in-screen fullscreen overlay */ }
                is StudentEffect.OpenUrl          -> {
                    try { uriHandler.openUri(effect.url) }
                    catch (_: Exception) { snackbarHostState.showSnackbar("Не удалось открыть ссылку") }
                }
            }
        }
    }

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
            StudentBottomNavBar(
                selectedTab = state.selectedTab,
                strings = getStrings(state.currentLanguage),
                onTabSelected = { vm.onEvent(StudentEvent.NavigateToTab(it)) }
            )
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

            // Main content — animated tab transitions
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    val isForward = targetState.ordinal >= initialState.ordinal
                    (slideInHorizontally(tween(260)) { if (isForward) it / 4 else -it / 4 } +
                     fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(200)) { if (isForward) -it / 4 else it / 4 } +
                     fadeOut(tween(200)))
                },
                label = "student_tab"
            ) { tab ->
                when (tab) {
                    StudentTab.Home    -> StudentHomeTab(    state = state, vm = vm, onLogout = onLogout, modifier = Modifier.padding(innerPadding))
                    StudentTab.Help    -> StudentHelpTab(    state = state,           modifier = Modifier.padding(innerPadding))
                    StudentTab.Courses -> StudentCoursesTab( state = state, vm = vm,  modifier = Modifier.padding(innerPadding))
                    StudentTab.Profile -> StudentProfileTab(
                        state = state,
                        onLogout = onLogout,
                        onLanguageChange = { vm.onEvent(StudentEvent.ChangeLanguage(it)) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            // ── Text Course Viewer overlay ─────────────────────────────────────
            if (state.showTextCourseViewer && state.selectedAddedCourse != null) {
                TextCourseViewerDialog(
                    course = state.selectedAddedCourse!!,
                    onDismiss = { vm.onEvent(StudentEvent.CloseTextCourse) }
                )
            }

            // ── Student Test full-screen overlay ──────────────────────────────
            val activeTestState = state.activeTestState
            if (activeTestState != null) {
                StudentTestScreen(
                    testState = activeTestState,
                    vm = vm
                )
            }

            // ── Floating AI Chat Button ────────────────────────────────────────
            if (activeTestState == null) {
                AiChatFab(
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
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(320)) { it } + fadeIn(tween(280)),
                exit  = slideOutVertically(tween(260)) { it } + fadeOut(tween(220))
            ) {
                AiChatOverlay(
                    state = state,
                    vm = vm,
                    onDismiss = { vm.onEvent(StudentEvent.CloseAiChat) }
                )
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
                        Text("📝 Текстовый курс", color = PsychTeal, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
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
                            "Автор: ${course.createdByName}",
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
                        Text("Текст курса отсутствует", color = Color.White.copy(0.35f), fontSize = 14.sp)
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
                    Text("Закрыть", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
    hasMessages: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue  = 0.9f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_alpha"
    )

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
                        listOf(Color(0xFF9D5FF5).copy(glowAlpha * 0.35f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // Main button
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF9D5FF5), PsychTeal)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 24.sp)
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
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    val sheetHeightFraction by animateFloatAsState(
        targetValue = if (isFullscreen) 0.97f else 0.78f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "ai_chat_sheet_height"
    )

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        // Dim background — clicking outside closes chat
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Chat panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(sheetHeightFraction)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Color(0xFF0B0B1E))
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(Color(0xFF9D5FF5).copy(0.5f), PsychTeal.copy(0.4f))),
                    RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            // Drag handle
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .background(Color.White.copy(0.15f), RoundedCornerShape(2.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isFullscreen = !isFullscreen }
                    )
                    .align(Alignment.CenterHorizontally)
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF9D5FF5).copy(0.4f), PsychTeal.copy(0.2f))),
                            CircleShape
                        )
                        .border(1.dp, Brush.linearGradient(listOf(Color(0xFF9D5FF5), PsychTeal)), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("🤖", fontSize = 18.sp) }

                Column(Modifier.weight(1f)) {
                    Text("AI Ассистент", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (isFullscreen) "Полноэкранный режим • ${state.chatMessages.size} сообщ."
                        else "Компактный режим • ${state.chatMessages.size} сообщ.",
                        color = PsychTeal.copy(0.8f),
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.07f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { isFullscreen = !isFullscreen }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isFullscreen) "🗗" else "🗖",
                        color = Color.White.copy(0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.chatMessages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(0.07f))
                            .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { vm.onEvent(StudentEvent.ClearChatHistory) }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("🗑", color = Color.White.copy(0.6f), fontSize = 13.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.07f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White.copy(0.55f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color(0xFF9D5FF5).copy(0.2f), thickness = 1.dp)

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.chatMessages.isEmpty()) {
                    item { AiChatOverlayEmptyState() }
                }
                items(state.chatMessages) { msg ->
                    ChatBubble(message = msg)
                }
                if (state.isChatLoading) {
                    item { TypingIndicatorOverlay() }
                }
            }

            // Error banner
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

            // Input
            AiChatInputBar(
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
}

@Composable
private fun AiChatOverlayEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🤖", fontSize = 48.sp)
        Text(
            "AI Ассистент готов помочь",
            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Text(
            "Задайте любой вопрос о здоровье,\nучёбе или психологическом благополучии",
            color = Color.White.copy(0.45f), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp
        )
    }
}

@Composable
private fun TypingIndicatorOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_overlay")
    Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Brush.radialGradient(listOf(PsychTeal.copy(0.3f), Color(0xFF9D5FF5).copy(0.15f))), CircleShape)
                .border(1.dp, PsychTeal.copy(0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("🤖", fontSize = 13.sp) }

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
    value: String,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val overLimit = (value.length / 4) > 50_000
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var isInputFocused by remember { mutableStateOf(false) }

    fun requestInputFocusAndKeyboard() {
        focusRequester.requestFocus()
        keyboardController?.show()
        scope.launch { bringIntoViewRequester.bringIntoView() }
    }

    LaunchedEffect(Unit) {
        requestInputFocusAndKeyboard()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050010).copy(0.95f))
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
                modifier      = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusChanged { focusState ->
                        isInputFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            keyboardController?.show()
                            scope.launch { bringIntoViewRequester.bringIntoView() }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (!isInputFocused) {
                                    requestInputFocusAndKeyboard()
                                } else {
                                    keyboardController?.show()
                                    scope.launch { bringIntoViewRequester.bringIntoView() }
                                }
                            }
                        )
                    },
                placeholder   = { Text("Задайте вопрос AI...", color = Color.White.copy(0.35f), fontSize = 14.sp) },
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

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(0.07f))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { requestInputFocusAndKeyboard() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("⌨", color = Color.White.copy(0.75f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

