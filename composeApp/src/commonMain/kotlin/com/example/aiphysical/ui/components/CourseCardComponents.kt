package com.example.aiphysical.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.BaseCourseCatalogItem
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.ui.theme.PsychCritical
import com.example.aiphysical.ui.theme.PsychTeal
import com.example.aiphysical.ui.theme.TextHint
import com.example.aiphysical.ui.theme.TextPrimary
import com.example.aiphysical.ui.theme.TextSecondary

@Composable
fun PlatformCourseCard(
    course: BaseCourseCatalogItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    metaText: String = course.durationLabel,
    badgeText: String? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val color = Color(course.accentColorHex)
    val safeProgress = progress?.coerceIn(0f, 1f)
    val animProgress by animateFloatAsState(
        targetValue = safeProgress ?: 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "platform_course_${course.id}"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.09f), Color.White.copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(color.copy(0.35f), color.copy(0.08f))), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(Brush.radialGradient(listOf(color.copy(0.40f), color.copy(0.10f))), RoundedCornerShape(14.dp))
                    .border(1.dp, color.copy(0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(course.emoji, fontSize = 24.sp)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        course.title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!badgeText.isNullOrBlank()) {
                        Box(
                            Modifier
                                .background(color.copy(0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, color.copy(0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(badgeText, color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Text(
                    course.description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (safeProgress != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color.copy(0.15f))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(animProgress)
                                    .fillMaxHeight()
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                        }
                        Text("${(safeProgress * 100).toInt()}%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(metaText, color = TextHint, fontSize = 11.sp)
                }
            }

            Box(
                Modifier
                    .size(34.dp)
                    .background(color.copy(0.15f), CircleShape)
                    .border(1.dp, color.copy(0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if ((safeProgress ?: 0f) > 0f) "▶" else "↗", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        footer?.invoke(this)
    }
}

@Composable
fun OrganizationCourseCard(
    course: OrganizationCourse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDeleteButton: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    val typeColor = if (course.type == CourseContentType.VIDEO) Color(0xFFFF8C00) else PsychTeal
    val typeLabel = if (course.type == CourseContentType.VIDEO) "Видео" else "Текстовый"
    val typeEmoji = if (course.type == CourseContentType.VIDEO) "🎬" else "📝"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(typeColor.copy(0.35f), typeColor.copy(0.08f))), RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(typeColor.copy(0.18f), RoundedCornerShape(12.dp))
                .border(1.dp, typeColor.copy(0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(typeEmoji, fontSize = 20.sp) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(course.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(typeColor.copy(0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, typeColor.copy(0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(typeLabel, color = typeColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (course.createdByName.isNotBlank()) {
                    Text(course.createdByName, color = TextHint, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (course.description.isNotBlank()) {
                Text(course.description, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (showDeleteButton && onDelete != null) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(PsychCritical.copy(0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, PsychCritical.copy(0.4f), RoundedCornerShape(8.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete),
                contentAlignment = Alignment.Center
            ) { Text("🗑", fontSize = 14.sp) }
        } else {
            Text("›", color = typeColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

