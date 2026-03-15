package com.example.aiphysical.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.TrendPoint
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.ui.theme.*
import com.example.aiphysical.ui.theme.*

@Composable
fun EmotionalTrendChart(
    trendPoints: List<TrendPoint>,
    stressLabel: String,
    burnoutLabel: String,
    modifier: Modifier = Modifier,
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(trendPoints) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1800, easing = FastOutSlowInEasing))
    }
    val progress by animProgress.asState()

    Column(modifier = modifier) {
        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            LegendDot(color = VioletGlow, label = stressLabel)
            LegendDot(color = AccentPink, label = burnoutLabel)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            if (trendPoints.isEmpty()) return@Canvas

            val padL = 32f; val padR = 16f; val padT = 8f; val padB = 28f
            val w = size.width - padL - padR
            val h = size.height - padT - padB
            val n = trendPoints.size

            // Grid lines (horizontal)
            for (i in 0..4) {
                val y = padT + h * (1f - i / 4f)
                drawLine(GlassBorder.copy(alpha = 0.3f), Offset(padL, y), Offset(padL + w, y), strokeWidth = 0.5f)
                drawContext.canvas.nativeCanvas.apply { /* Y-axis labels drawn via Text composable */ }
            }

            // Stress Path
            val stressPath = buildLinePath(trendPoints.map { it.stressValue }, n, w, h, padL, padT, progress)
            drawNeonPath(stressPath, VioletGlow)

            // Burnout Path
            val burnoutPath = buildLinePath(trendPoints.map { it.burnoutValue }, n, w, h, padL, padT, progress)
            drawNeonPath(burnoutPath, AccentPink)

            // Axis
            drawLine(GlassBorderBright.copy(alpha = 0.6f), Offset(padL, padT), Offset(padL, padT + h), strokeWidth = 1f)
            drawLine(GlassBorderBright.copy(alpha = 0.6f), Offset(padL, padT + h), Offset(padL + w, padT + h), strokeWidth = 1f)

            // X-axis dots for every 5 days
            val visibleCount = (n * progress).toInt().coerceAtLeast(1)
            for (i in 0 until visibleCount step 5) {
                val x = padL + i.toFloat() / (n - 1) * w
                drawCircle(GlassBorderBright.copy(alpha = 0.5f), radius = 2f, center = Offset(x, padT + h))
            }
        }
    }
}

private fun buildLinePath(
    values: List<Float>,
    n: Int,
    w: Float,
    h: Float,
    padL: Float,
    padT: Float,
    progress: Float
): Path {
    val path = Path()
    val visibleCount = (n * progress).toInt().coerceAtLeast(1)
    values.take(visibleCount).forEachIndexed { i, v ->
        val x = padL + i.toFloat() / (n - 1).coerceAtLeast(1) * w
        val y = padT + h * (1f - v / 100f)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    return path
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeonPath(path: Path, color: Color) {
    // Outer glow
    drawPath(path, color.copy(alpha = 0.10f), style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color.copy(alpha = 0.20f), style = Stroke(width = 8f,  cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color.copy(alpha = 0.45f), style = Stroke(width = 4f,  cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Core line
    drawPath(path, color.copy(alpha = 0.95f), style = Stroke(width = 2f,  cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape).border(1.dp, color.copy(alpha = 0.6f), CircleShape))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

// ── Status Badge ──────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(
    status: String,
    label: String,
    modifier: Modifier = Modifier,
    showEmoji: Boolean = true,
) {
    val (color, emoji) = when (status) {
        "normal" -> Pair(SuccessColor, "✅")
        "stress" -> Pair(Color(0xFFFFB800), "⚠️")
        "critical" -> Pair(ErrorColor, "🔴")
        else -> Pair(TextHint, "❓")
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showEmoji) Text(emoji, fontSize = 11.sp)
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── KPI Card ──────────────────────────────────────────────────────────────────

@Composable
fun KpiCard(
    label: String,
    value: Float,
    emoji: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val animValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "kpi_anim"
    )
    Column(
        modifier = modifier
            .glassCard(cornerRadius = 16.dp, bgAlpha = 0.10f, borderAlpha = 0.20f)
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(
            text = "${animValue.toInt()}%",
            color = accentColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            lineHeight = 13.sp, maxLines = 2, modifier = Modifier.padding(horizontal = 4.dp))
        // Progress bar
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (animValue / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.5f))),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// ── Invite Code Card ──────────────────────────────────────────────────────────

@Composable
fun InviteCodeCard(
    label: String,
    code: String,
    emoji: String,
    accentColor: Color,
    copyLabel: String,
    shareLabel: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.18f), accentColor.copy(alpha = 0.08f))),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(emoji, fontSize = 20.sp)
            Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        // Code display with neon text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code.ifBlank { "——" },
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.7f)))
                )
            )
        }
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CodeActionButton(emoji = "📋", label = copyLabel, color = accentColor, onClick = onCopy, modifier = Modifier.weight(1f))
            CodeActionButton(emoji = "📤", label = shareLabel, color = accentColor, onClick = onShare, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CodeActionButton(emoji: String, label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
    ) {
        Text("$emoji $label", color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Section Header ─────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, emoji: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(emoji, fontSize = 18.sp)
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(message: String, emoji: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp, bgAlpha = 0.07f)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 36.sp)
        Text(message, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

// ── Glass Search Bar ───────────────────────────────────────────────────────────

@Composable
fun GlassSearchBar(
    query: String,
    hint: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🔍", fontSize = 16.sp)
        BasicTextField(
            value = query, onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) Text(hint, color = TextHint, fontSize = 14.sp)
                inner()
            }
        )
        if (query.isNotBlank()) {
            Text("✕", color = TextHint, fontSize = 14.sp,
                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onQueryChange("") })
        }
    }
}

// ── Member List Card ───────────────────────────────────────────────────────────

@Composable
fun MemberListCard(
    member: UserProfile,
    statusLabel: String,
    detailsLabel: String,
    roleLabel: String,
    ageLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 18.dp, bgAlpha = 0.07f, borderAlpha = 0.15f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(42.dp)
                .background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(NeonViolet.copy(alpha = 0.4f), CardSurface)), CircleShape)
                .border(1.dp, NeonViolet.copy(0.4f), CircleShape),
            Alignment.Center
        ) {
            Text(member.fullName.take(1).uppercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(member.fullName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$roleLabel: ${member.role}", color = TextSecondary, fontSize = 11.sp)
                if (member.ageGroup.isNotBlank()) Text("· ${member.ageGroup}", color = TextHint, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusBadge(status = member.latestAiStatus, label = statusLabel)
            Text("›", color = NeonViolet, fontSize = 18.sp)
        }
    }
}

// ── Critical Alerts Panel ──────────────────────────────────────────────────────

@Composable
fun CriticalAlertsPanel(
    criticalMembers: List<UserProfile>,
    strings: com.example.aiphysical.ui.theme.Strings,
    onContactPsychologist: (UserProfile) -> Unit,
    onViewMember: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ErrorColor.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
            .border(1.5.dp, Brush.horizontalGradient(listOf(ErrorColor.copy(0.7f), ErrorColor.copy(0.3f))), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).background(ErrorColor, CircleShape))
            Text(strings.sectionCriticalAlerts, color = ErrorColor, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text("${criticalMembers.size}", color = ErrorColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        androidx.compose.material3.HorizontalDivider(color = ErrorColor.copy(alpha = 0.2f))
        criticalMembers.forEach { member ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ErrorColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, ErrorColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔴", fontSize = 14.sp)
                Column(Modifier.weight(1f)) {
                    Text(member.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(member.email, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                androidx.compose.material3.TextButton(
                    onClick = { onContactPsychologist(member) },
                    modifier = Modifier.background(ErrorColor.copy(0.18f), RoundedCornerShape(10.dp))
                ) {
                    Text("🧠 ${strings.contactPsychologist}", color = ErrorColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

