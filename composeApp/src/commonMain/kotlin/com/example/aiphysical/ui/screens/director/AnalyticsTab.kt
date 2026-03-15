package com.example.aiphysical.ui.screens.director

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.director.*
import com.example.aiphysical.ui.components.*
import com.example.aiphysical.ui.theme.*

@Composable
fun AnalyticsTab(
    state: DirectorDashboardState,
    vm: DirectorDashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)

    val displayedMembers = remember(state.members, state.analyticsFilter) {
        when (state.analyticsFilter) {
            "JUNIOR" -> state.members.filter { it.ageGroup.equals("JUNIOR", ignoreCase = true) }
            "MIDDLE" -> state.members.filter { it.ageGroup.equals("MIDDLE", ignoreCase = true) }
            "SENIOR" -> state.members.filter { it.ageGroup.equals("SENIOR", ignoreCase = true) }
            "STAFF"  -> state.members.filter { it.role == "psychologist" || it.role == "director" }
            else     -> state.members
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.12f), CyanAccent.copy(0.06f))))
                    .border(1.dp, Brush.horizontalGradient(listOf(NeonViolet.copy(0.4f), CyanAccent.copy(0.2f))), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    strings.tabAnalytics,
                    style = TextStyle(brush = Brush.horizontalGradient(listOf(NeonViolet, CyanAccent)), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "${state.members.size} ${strings.sectionMembers.lowercase()} · ${strings.sectionAnalytics}",
                    color = Color.White.copy(0.4f), fontSize = 11.sp
                )
            }
        }

        // ── Filter Chips ──────────────────────────────────────────────────────
        item {
            val filters = listOf(
                "ALL" to strings.filterAll,
                "JUNIOR" to strings.filterJunior,
                "MIDDLE" to strings.filterMiddle,
                "SENIOR" to strings.filterSenior,
                "STAFF"  to strings.filterStaff,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filters) { (key, label) ->
                    GlassFilterChip(
                        label = label,
                        isActive = state.analyticsFilter == key,
                        onClick = { vm.onEvent(DirectorEvent.SetAnalyticsFilter(key)) }
                    )
                }
            }
        }

        // ── Filtered count badge ──────────────────────────────────────────────
        item {
            AnimatedVisibility(visible = state.analyticsFilter != "ALL") {
                Row(
                    Modifier
                        .background(CyanAccent.copy(0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, CyanAccent.copy(0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).background(CyanAccent, CircleShape))
                    Text("Показано: ${displayedMembers.size}", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
                    CircularProgressIndicator(color = NeonViolet, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                }
            }
            return@LazyColumn
        }

        if (displayedMembers.isEmpty()) {
            item {
                GlassEmptyState(
                    emoji = "🔍",
                    title = strings.noMembers,
                    subtitle = "Измените фильтр для просмотра участников"
                )
            }
        } else {
            items(displayedMembers, key = { it.uid }) { member ->
                ExpandableMemberCard(
                    member = member,
                    strings = strings,
                    onViewDetails = { vm.onEvent(DirectorEvent.SelectMember(member)) }
                )
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

// ─── Glass Filter Chip ────────────────────────────────────────────────────────

@Composable
private fun GlassFilterChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "chip_$label")
    val glowAlpha by if (isActive) infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "chip_glow"
    ) else remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .then(
                if (isActive) Modifier.drawBehind {
                    drawRoundRect(NeonViolet.copy(alpha = 0.2f * glowAlpha), cornerRadius = CornerRadius(24.dp.toPx()))
                } else Modifier
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isActive) Brush.horizontalGradient(listOf(NeonViolet, CyanAccent.copy(0.85f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(0.06f), Color.White.copy(0.03f)))
            )
            .border(
                1.dp,
                if (isActive) NeonViolet.copy(0.8f) else Color.White.copy(0.12f),
                RoundedCornerShape(24.dp)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = if (isActive) Color.White else Color.White.copy(0.5f),
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ─── Glass Empty State ────────────────────────────────────────────────────────

@Composable
private fun GlassEmptyState(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 40.sp)
        Text(title, color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(subtitle, color = TextHint, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

// ─── Expandable Member Card ───────────────────────────────────────────────────

@Composable
fun ExpandableMemberCard(
    member: UserProfile,
    strings: Strings,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val statusColor = when (member.latestAiStatus) { "normal" -> StatusNormal; "stress" -> StatusStress; "critical" -> StatusCritical; else -> TextHint }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (expanded) Brush.verticalGradient(listOf(NeonViolet.copy(0.1f), Color.White.copy(0.04f)))
                else Brush.verticalGradient(listOf(Color.White.copy(0.05f), Color.White.copy(0.03f)))
            )
            .border(
                1.dp,
                if (expanded) Brush.linearGradient(listOf(NeonViolet.copy(0.5f), CyanAccent.copy(0.4f)))
                else Brush.linearGradient(listOf(Color.White.copy(0.14f), Color.White.copy(0.04f))),
                RoundedCornerShape(20.dp)
            )
            .animateContentSize(animationSpec = tween(300))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Collapsed header row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Avatar
            Box(
                Modifier.size(46.dp)
                    .background(Brush.radialGradient(listOf(NeonViolet.copy(0.5f), CardSurface)), CircleShape)
                    .border(1.5.dp, statusColor.copy(0.7f), CircleShape),
                Alignment.Center
            ) {
                Text(member.fullName.take(1).uppercase(), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(member.fullName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(member.email, color = Color.White.copy(0.35f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                StatusBadge(status = member.latestAiStatus, label = statusLabel(member.latestAiStatus, strings))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${member.burnoutScore.toInt()}%", color = MetricBurnout, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(if (expanded) "▲" else "▼", color = NeonViolet.copy(0.7f), fontSize = 9.sp)
                }
            }
        }

        // Expanded section
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(300)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.4f), CyanAccent.copy(0.3f), Color.Transparent))))

                // 5 metric bars
                MetricProgressBar(label = strings.metricBurnout,   value = member.burnoutScore,    isHighBad = true,  color = MetricBurnout)
                MetricProgressBar(label = strings.kpiStress,        value = member.stressScore,     isHighBad = true,  color = MetricStress)
                MetricProgressBar(label = strings.metricEmotion,   value = member.emotionScore,    isHighBad = false, color = MetricEmotion)
                MetricProgressBar(label = strings.metricMotivation, value = member.motivationScore, isHighBad = false, color = MetricMotivation)
                MetricProgressBar(label = strings.metricAnxiety,   value = member.anxietyScore,    isHighBad = true,  color = MetricAnxiety)

                Spacer(Modifier.height(2.dp))
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.2f), CyanAccent.copy(0.12f))), RoundedCornerShape(14.dp))
                        .border(1.dp, Brush.horizontalGradient(listOf(NeonViolet.copy(0.5f), CyanAccent.copy(0.4f))), RoundedCornerShape(14.dp))
                ) {
                    Text("${strings.viewDetails} →", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Metric Progress Bar ──────────────────────────────────────────────────────

@Composable
fun MetricProgressBar(
    label: String,
    value: Float,
    isHighBad: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 100f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "metric_$label"
    )
    val barColor = when {
        isHighBad -> when {
            animValue > 70f -> ErrorColor
            animValue > 40f -> AlertOrange
            else            -> SuccessColor
        }
        else -> when {
            animValue < 30f -> ErrorColor
            animValue < 60f -> AlertOrange
            else            -> SuccessColor
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = color.copy(0.8f), fontSize = 11.sp, modifier = Modifier.width(84.dp), maxLines = 1)
        Box(
            modifier = Modifier.weight(1f).height(7.dp).clip(CircleShape).background(Color.White.copy(0.07f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animValue / 100f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(barColor, barColor.copy(0.55f))))
            )
        }
        Text(
            "${animValue.toInt()}%", color = barColor, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp), textAlign = TextAlign.End
        )
    }
}
