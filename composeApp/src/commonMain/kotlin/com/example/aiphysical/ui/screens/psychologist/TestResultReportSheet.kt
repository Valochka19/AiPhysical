package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.presentation.psychologist.RecentTestFeedItem
import com.example.aiphysical.ui.components.UmiAvatarBadge
import com.example.aiphysical.ui.theme.*

/**
 * Full test report bottom-sheet dialog.
 * Displays per-student metric gauges and an AI-analysis placeholder.
 */
@Composable
fun TestResultReportSheet(
    item: RecentTestFeedItem,
    language: AppLanguage = AppLanguage.RU,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PsychBackground.copy(0.90f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MatteSurface)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PsychTeal.copy(0.4f), MatteCardBorder)),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* absorb touches */ }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ── Handle indicator ──────────────────────────────────────────
                Box(
                    Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(MatteCardBorder, RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                // ── Sheet header ──────────────────────────────────────────────
                val (statusColor, statusLabel) = when (item.studentStatus) {
                    "critical" -> PsychCritical to language.pick("Критично", "Critical", "Критикалық")
                    "stress"   -> PsychWarning  to language.pick("Стресс", "Stress", "Стресс")
                    "normal"   -> PsychTeal     to language.pick("Норма", "Normal", "Қалыпты")
                    else        -> TextHint      to language.pick("Неизвестно", "Unknown", "Белгісіз")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(language.pick("Отчёт по тесту", "Test report", "Тест есебі"), color = TextSecondary, fontSize = 13.sp)
                        Text(
                            language.pick("Психологическая оценка", "Psychological assessment", "Психологиялық бағалау"),
                            color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Box(
                        Modifier
                            .background(statusColor, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(statusLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // ── Student identity row ──────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PsychBackground)
                        .border(1.dp, MatteCardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .background(statusColor.copy(0.15f), CircleShape)
                            .border(2.dp, statusColor.copy(0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            item.studentName.take(1).uppercase(),
                            color = statusColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column {
                        Text(item.studentName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(language.pick("Студент", "Student", "Студент"), color = TextSecondary, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = MatteCardBorder)

                // ── Metrics gauges ────────────────────────────────────────────
                Text(
                    language.pick("ПОКАЗАТЕЛИ", "METRICS", "КӨРСЕТКІШТЕР"),
                    color = TextHint, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ReportGauge(language.pick("Выгорание", "Burnout", "Күйіп кету"), item.burnoutScore, MetricBurnout)
                    ReportGauge(language.pick("Стресс", "Stress", "Стресс"), item.stressScore, MetricStress)
                    ReportGauge(language.pick("Тревога", "Anxiety", "Мазасыздық"), item.anxietyScore, MetricAnxiety)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ReportGauge(language.pick("Состояние", "Condition", "Жағдай"), item.emotionScore, MetricEmotion)
                    ReportGauge(language.pick("Мотивация", "Motivation", "Мотивация"), item.motivationScore, MetricMotivation)
                }

                HorizontalDivider(color = MatteCardBorder)

                // ── AI Analysis placeholder ───────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(PsychBackground)
                        .border(1.dp, Color(0xFF8A2BE2).copy(0.5f), RoundedCornerShape(18.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UmiAvatarBadge(
                            modifier = Modifier.size(28.dp),
                            backgroundColor = Color(0xFF8A2BE2).copy(0.14f),
                            borderColor = Color(0xFF8A2BE2).copy(0.45f),
                            imagePadding = 3.dp,
                            contentDescription = language.pick("Уми", "Umi", "Уми")
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                language.pick("AI-анализ теста (Gemini)", "AI test analysis (Gemini)", "Тесттің AI талдауы (Gemini)"),
                                color = Color(0xFF9D5FF5), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                language.pick("Интеллектуальная интерпретация результатов", "Intelligent interpretation of results", "Нәтижелердің зияткерлік интерпретациясы"),
                                color = TextHint, fontSize = 11.sp
                            )
                        }
                        Box(
                            Modifier
                                .background(Color(0xFF8A2BE2).copy(0.2f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF8A2BE2).copy(0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(language.pick("Скоро", "Soon", "Жақында"), color = Color(0xFF9D5FF5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF8A2BE2).copy(0.18f))

                    // Placeholder text
                    Text(
                        language.pick(
                            "Здесь будет интеллектуальный анализ результатов от модели Gemini AI.\n\nСистема автоматически выявит психологические паттерны, оценит риски выгорания, стресса и тревожности, а также предложит персональные рекомендации для данного студента.",
                            "An intelligent analysis of the results from the Gemini AI model will appear here.\n\nThe system will automatically identify psychological patterns, assess the risks of burnout, stress, and anxiety, and provide personalized recommendations for this student.",
                            "Мұнда Gemini AI моделінен алынған нәтижелердің зияткерлік талдауы көрсетіледі.\n\nЖүйе психологиялық үлгілерді автоматты түрде анықтап, күйіп кету, стресс және мазасыздық тәуекелдерін бағалап, осы студентке жеке ұсыныстар береді."
                        ),
                        color = TextHint,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    // Visual placeholder bars (shimmer-style)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.85f, 0.65f, 0.75f).forEach { w ->
                            Box(
                                Modifier
                                    .fillMaxWidth(w)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF8A2BE2).copy(0.12f))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Gauge component local to this sheet ──────────────────────────────────────

@Composable
private fun ReportGauge(label: String, value: Float, color: Color) {
    val animVal by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "report_gauge_$label"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(80.dp)) {
                val stroke = 8.dp.toPx()
                drawArc(
                    color = color.copy(0.12f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * (animVal / 100f),
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                "${value.toInt()}%",
                color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

