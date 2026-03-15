package com.example.aiphysical.ui.screens.director

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.KpiData
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.director.*
import com.example.aiphysical.ui.components.*
import com.example.aiphysical.ui.theme.*

// ─── Dashboard Tab ────────────────────────────────────────────────────────────

@Composable
fun DashboardTab(
    state: DirectorDashboardState,
    vm: DirectorDashboardViewModel,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)

    // Trigger gauge animation only after data loads
    var gaugesVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) { if (!state.isLoading) gaugesVisible = true }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // ── Responsive breakpoint: < 580dp height = compact (scrollable) ──────
        val isCompact = maxHeight < 580.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isCompact) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = if (isCompact) 36.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── 1. Floating Header ─────────────────────────────────────────────
            FloatingDirectorHeader(
                strings = strings,
                currentLanguage = state.currentLanguage,
                isLoading = state.isLoading,
                onLanguageChange = { vm.onEvent(DirectorEvent.ChangeLanguage(it)) },
                onLogout = onLogout
            )

            if (state.isLoading) {
                DashboardLoadingSkeleton(
                    modifier = if (!isCompact) Modifier.weight(1f) else Modifier
                )
            } else {
                // ── 2. AI Analytics Card ────────────────────────────────────────
                CleanAiInsightCard(
                    insightText = state.aiInsightText,
                    isLoading = state.isAiLoading,
                    onRefresh = { vm.onEvent(DirectorEvent.LoadAiInsight) }
                )

                // ── 3. KPI Gauges Row ───────────────────────────────────────────
                KpiGaugeRow(
                    kpiData = state.kpiData,
                    strings = strings,
                    visible = gaugesVisible
                )

                // ── 4. Average Metrics (fills remaining space on large screens) ─
                AverageMetricsCard(
                    kpiData = state.kpiData,
                    strings = strings,
                    expanded = !isCompact,
                    modifier = if (!isCompact) Modifier.weight(1f) else Modifier
                )
            }
        }
    }

    // Contact dialog
    if (state.showContactDialog) {
        ContactPsychologistDialog(
            targetMember = state.contactTargetMember,
            psychologists = state.psychologists,
            strings = strings,
            onDismiss = { vm.onEvent(DirectorEvent.DismissContactDialog) }
        )
    }
}

// ─── 1. Floating Director Header ──────────────────────────────────────────────

@Composable
private fun FloatingDirectorHeader(
    strings: Strings,
    currentLanguage: com.example.aiphysical.presentation.auth.AppLanguage,
    isLoading: Boolean,
    onLanguageChange: (com.example.aiphysical.presentation.auth.AppLanguage) -> Unit,
    onLogout: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "title_glow")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 80f, targetValue = 150f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_r"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.32f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_a"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Action row — Language + Logout
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            LanguageSwitcher(currentLanguage = currentLanguage, onLanguageChange = onLanguageChange)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) CircularProgressIndicator(Modifier.size(13.dp), color = CyanAccent, strokeWidth = 2.dp)
                FloatingActionBtn(label = strings.logoutBtn, onClick = onLogout)
            }
        }

        // KACY title — no card, floating on background
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "KACY",
                modifier = Modifier.drawBehind {
                    val cx = size.width * 0.3f
                    val cy = size.height * 0.5f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyanAccent.copy(alpha = glowAlpha), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = Offset(cx, cy)
                    )
                },
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        listOf(Color.White, CyanAccent.copy(0.9f), Color.White.copy(0.8f))
                    ),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
            )
            Text(
                text = strings.directorPanelSubtitle,
                color = Color.White.copy(0.38f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun FloatingActionBtn(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
        modifier = Modifier
            .background(Color.White.copy(0.06f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.11f), RoundedCornerShape(12.dp))
    ) {
        Text(label, color = Color.White.copy(0.45f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── 2. Clean AI Insights Card ─────────────────────────────────────────────────

@Composable
fun AiAnalysisCard(
    insightText: String, isLoading: Boolean, title: String, orgTitle: String,
    loadingText: String, emptyText: String, onRefresh: () -> Unit, modifier: Modifier = Modifier,
) = CleanAiInsightCard(insightText, isLoading, onRefresh, modifier)

@Composable
private fun CleanAiInsightCard(
    insightText: String,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.05f))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(0.20f), Color.Transparent)),
                RoundedCornerShape(24.dp)
            )
            .then(if (isLoading) Modifier.shimmerEffect() else Modifier)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
            Text(
                text = "AI-Аналитик подготовил для вас общий анализ персонала и студентов",
                color = Color.White.copy(0.50f),
                fontSize = 11.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(end = 10.dp)
            )
            if (!isLoading) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanAccent.copy(0.07f))
                        .border(1.dp, CyanAccent.copy(0.18f), RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRefresh
                        ),
                    Alignment.Center
                ) { Text("↻", color = CyanAccent.copy(0.75f), fontSize = 14.sp) }
            }
        }
        // Cyan hairline divider
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(CyanAccent.copy(0.28f), Color.White.copy(0.06f), Color.Transparent)
                )
            )
        )
        // Content
        if (isLoading) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Анализируем данные...", color = Color.White.copy(0.22f), fontSize = 11.sp)
                repeat(3) { i ->
                    Box(
                        Modifier
                            .fillMaxWidth(if (i == 2) 0.58f else 1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(0.07f))
                    )
                }
            }
        } else {
            Text(
                text = insightText.ifBlank { "Нет данных для анализа." },
                color = Color.White.copy(0.80f),
                fontSize = 13.sp,
                lineHeight = 21.sp
            )
        }
    }
}

// ─── 3. KPI Gauge Row ─────────────────────────────────────────────────────────

@Composable
private fun KpiGaugeRow(kpiData: KpiData, strings: Strings, visible: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularGaugeWidget(
            value = if (visible) kpiData.burnoutIndex else 0f,
            label = strings.kpiBurnout,
            accentColor = MetricBurnout,
            modifier = Modifier.weight(1f)
        )
        CircularGaugeWidget(
            value = if (visible) kpiData.avgStressLevel else 0f,
            label = strings.kpiStress,
            accentColor = StatusStress,
            modifier = Modifier.weight(1f)
        )
        CircularGaugeWidget(
            value = if (visible) kpiData.courseEngagement else 0f,
            label = strings.kpiEngagement,
            accentColor = CyanAccent,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── Circular Gauge KPI Widget ────────────────────────────────────────────────

@Composable
fun CircularGaugeWidget(
    value: Float,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val animValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 100f),
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "gauge_$label"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.05f))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(accentColor.copy(0.35f), Color.White.copy(0.04f))),
                RoundedCornerShape(20.dp)
            )
            .padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeW = 6.dp.toPx()
                val radius  = (size.minDimension - strokeW) / 2f
                val center  = Offset(size.width / 2f, size.height / 2f)
                val tl      = Offset(center.x - radius, center.y - radius)
                val sz      = Size(radius * 2f, radius * 2f)
                drawArc(
                    color = accentColor.copy(0.12f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(strokeW, cap = StrokeCap.Round), topLeft = tl, size = sz
                )
                if (animValue > 0f) drawArc(
                    brush = Brush.sweepGradient(
                        listOf(accentColor.copy(0.55f), accentColor, accentColor.copy(0.6f)), center
                    ),
                    startAngle = -90f, sweepAngle = (animValue / 100f) * 360f, useCenter = false,
                    style = Stroke(strokeW, cap = StrokeCap.Round), topLeft = tl, size = sz
                )
            }
            Text(
                text = "${animValue.toInt()}%",
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = label,
            color = Color.White.copy(0.42f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

// ─── 4. Average Metrics Card ──────────────────────────────────────────────────

@Composable
private fun AverageMetricsCard(
    kpiData: KpiData,
    strings: Strings,
    expanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val metrics = listOf(
        Triple(strings.metricBurnout,    kpiData.avgBurnout,     MetricBurnout),
        Triple(strings.metricStress,     kpiData.avgStressLevel, MetricStress),
        Triple(strings.metricEmotion,    kpiData.avgEmotion,     MetricEmotion),
        Triple(strings.metricMotivation, kpiData.avgMotivation,  MetricMotivation),
        Triple(strings.metricAnxiety,    kpiData.avgAnxiety,     MetricAnxiety),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.05f))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(0.20f), Color.Transparent)),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        // ── Card header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Средние показатели",
                color = Color.White.copy(0.65f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
            // Subtle "5 метрик" badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyanAccent.copy(0.08f))
                    .border(1.dp, CyanAccent.copy(0.18f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "5 метрик",
                    color = CyanAccent.copy(0.75f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // Cyan hairline
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(CyanAccent.copy(0.18f), Color.White.copy(0.06f), Color.Transparent)
                )
            )
        )
        Spacer(Modifier.height(if (expanded) 0.dp else 12.dp))

        // ── Metric rows ────────────────────────────────────────────────────────
        if (expanded) {
            // Fill remaining card height — distribute rows evenly
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                metrics.forEach { (label, value, color) ->
                    MetricBarRow(label = label, value = value, accentColor = color)
                }
            }
        } else {
            // Natural height — fixed spacing
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                metrics.forEach { (label, value, color) ->
                    MetricBarRow(label = label, value = value, accentColor = color)
                }
            }
        }
    }
}

@Composable
private fun MetricBarRow(label: String, value: Float, accentColor: Color) {
    val animValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 100f),
        animationSpec = tween(1300, easing = FastOutSlowInEasing),
        label = "avg_bar_$label"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Label — fixed width so all bars align
        Text(
            text = label,
            color = Color.White.copy(0.60f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(82.dp)
        )

        // Progress bar track + fill + glow
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.08f))
        ) {
            // Filled portion
            if (animValue > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animValue / 100f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor.copy(0.75f), accentColor)
                            )
                        )
                )
                // Glow — right-edge bloom effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animValue / 100f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                0.55f to Color.Transparent,
                                1.0f to accentColor.copy(0.55f)
                            )
                        )
                )
                // Top shine
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animValue / 100f)
                        .fillMaxHeight(0.45f)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.12f))
                )
            }
        }

        // Percentage — right-aligned, fixed width
        Text(
            text = "${animValue.toInt()}%",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp)
        )
    }
}

// ─── Loading Skeleton ──────────────────────────────────────────────────────────

@Composable
private fun DashboardLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // AI card skeleton
        Box(
            Modifier.fillMaxWidth().height(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(0.05f))
                .shimmerEffect()
        )
        // KPI row skeleton
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Box(
                    Modifier.weight(1f).height(118.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(0.05f))
                        .shimmerEffect()
                )
            }
        }
        // Metrics card skeleton — fills remaining space if weight is set
        Box(
            Modifier.fillMaxWidth()
                .then(if (modifier == Modifier) Modifier.height(200.dp) else Modifier.weight(1f))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(0.05f))
                .shimmerEffect()
        )
    }
}

// ─── Legacy public stubs ───────────────────────────────────────────────────────

@Composable
fun InviteToOrgButton(label: String, onClick: () -> Unit) { /* removed from home */ }

@Composable
fun MatteLoadingCard() {
    Box(
        Modifier.fillMaxWidth().height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.05f))
            .shimmerEffect()
    )
}

// ─── Contact Psychologist Dialog ───────────────────────────────────────────────

@Composable
private fun ContactPsychologistDialog(
    targetMember: UserProfile?,
    psychologists: List<UserProfile>,
    strings: Strings,
    onDismiss: () -> Unit,
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    strings.contactDialogTitle,
                    style = TextStyle(
                        brush = Brush.horizontalGradient(listOf(NeonViolet, CyanAccent)),
                        fontSize = 17.sp, fontWeight = FontWeight.Bold
                    )
                )
                targetMember?.let { Text("→ ${it.fullName}", color = Color.White.copy(0.38f), fontSize = 12.sp) }
            }
        },
        text = {
            if (psychologists.isEmpty()) {
                Text(strings.contactDialogNoPs, color = Color.White.copy(0.5f))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    psychologists.forEach { psych ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(0.05f))
                                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(psych.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(psych.email, color = Color.White.copy(0.38f), fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = {
                                    uriHandler.openUri("mailto:${psych.email}?subject=Consultation")
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .background(CyanAccent.copy(0.1f), RoundedCornerShape(10.dp))
                                    .border(1.dp, CyanAccent.copy(0.25f), RoundedCornerShape(10.dp))
                            ) { Text(strings.contactDialogEmail, color = CyanAccent, fontSize = 11.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) { Text(strings.close, color = Color.White.copy(0.45f)) }
        }
    )
}
