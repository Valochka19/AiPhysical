@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aiphysical.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
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
                is StudentEffect.NavigateToTest   -> { /* Burnout opens as overlay; other tests handled here in future */ }
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
                        androidx.compose.foundation.shape.CircleShape
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
                        androidx.compose.foundation.shape.CircleShape
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
                    StudentTab.Chat    -> StudentAiChatTab(  state = state, vm = vm,  modifier = Modifier.padding(innerPadding))
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

            // ── Burnout Test full-screen overlay ──────────────────────────────
            val burnoutState = state.burnoutTestState
            if (burnoutState != null) {
                BurnoutTestScreen(
                    testState = burnoutState,
                    vm        = vm
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
        NavItem(StudentTab.Chat,    "🤖", "AI Чат"),
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
    val indicatorAlpha by androidx.compose.animation.core.animateFloatAsState(
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
