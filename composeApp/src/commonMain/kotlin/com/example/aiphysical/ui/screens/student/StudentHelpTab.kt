package com.example.aiphysical.ui.screens.student

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.presentation.student.StudentUiState
import com.example.aiphysical.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
//  Student Help Tab — Contact the Psychologist
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun StudentHelpTab(
    state: StudentUiState,
    onOpenPsychologistChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = state.currentLanguage
    val helpOptions = listOf(
        Triple("📝", language.pick("Написать запрос", "Write a request", "Сұраныс жазу"), language.pick("Опиши своё состояние психологу", "Describe your condition to the psychologist", "Жағдайыңызды психологқа сипаттаңыз")),
        Triple("📅", language.pick("Записаться на сессию", "Book a session", "Сессияға жазылу"), language.pick("Онлайн или офлайн встреча", "Online or offline meeting", "Онлайн немесе офлайн кездесу")),
        Triple("🆘", language.pick("Срочная поддержка", "Urgent support", "Шұғыл қолдау"), language.pick("Нужна немедленная помощь", "Immediate help is needed", "Дереу көмек қажет")),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    language.pick("ЦЕНТР ПОМОЩИ", "HELP CENTER", "КӨМЕК ОРТАЛЫҒЫ"),
                    color = TextHint,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.8.sp
                )
                Text(
                    language.pick("Обратиться к психологу", "Contact a psychologist", "Психологқа жүгіну"),
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    language.pick(
                        "Ты можешь обратиться к психологу своей организации напрямую",
                        "You can directly contact your organization's psychologist",
                        "Ұйымыңыздың психологына тікелей жүгіне аласыз"
                    ),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }

        item(key = "contact") {
            PsychologistContactCard(language = language, onOpenPsychologistChat = onOpenPsychologistChat)
        }

        item(key = "divider_1") {
            HorizontalDivider(color = Color.White.copy(0.08f))
        }

        item(key = "quick_help_title") {
            Text(
                language.pick("БЫСТРАЯ ПОМОЩЬ", "QUICK HELP", "ЖЕДЕЛ КӨМЕК"),
                color = TextHint,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.8.sp
            )
        }

        items(helpOptions.size, key = { index -> helpOptions[index].first + helpOptions[index].second }) { index ->
            val (emoji, title, desc) = helpOptions[index]
            HelpOptionCard(emoji = emoji, title = title, description = desc)
        }

        item(key = "divider_2") {
            HorizontalDivider(color = Color.White.copy(0.08f))
        }

        item(key = "crisis") {
            CrisisResourceCard(language = language)
        }
    }
}

@Composable
private fun PsychologistContactCard(
    language: com.example.aiphysical.presentation.auth.AppLanguage,
    onOpenPsychologistChat: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(PsychTeal.copy(0.14f), PsychTeal.copy(0.04f))))
            .border(1.5.dp, Brush.horizontalGradient(listOf(PsychTeal.copy(0.7f), PsychTeal.copy(0.2f))), RoundedCornerShape(22.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenPsychologistChat)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Psychologist avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.radialGradient(listOf(PsychTeal.copy(0.55f), PsychTeal.copy(0.10f))),
                        CircleShape
                    )
                    .border(2.dp, PsychTeal.copy(0.70f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🧠", fontSize = 26.sp) }

            Column(Modifier.weight(1f)) {
                Text(language.pick("Психолог организации", "Organization psychologist", "Ұйым психологы"), color = PsychTeal.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Text(language.pick("Специалист", "Specialist", "Маман"), color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(7.dp).background(PsychTeal, CircleShape))
                    Text(language.pick("Онлайн", "Online", "Онлайн"), color = PsychTeal, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        HorizontalDivider(color = PsychTeal.copy(0.15f))

        // Send message (placeholder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(0.06f))
                .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(12.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenPsychologistChat)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                language.pick("Напиши своё сообщение…", "Write your message…", "Хабарламаңызды жазыңыз…"),
                color = TextHint,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .size(34.dp)
                    .background(PsychTeal, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text("→", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }

        Text(
            language.pick("💬 Открыть личный чат с психологом", "💬 Open a private chat with the psychologist", "💬 Психологпен жеке чатты ашу"),
            color = PsychTeal.copy(0.9f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HelpOptionCard(emoji: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.03f))))
            .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(Color.White.copy(0.10f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 20.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Text("›", color = TextHint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CrisisResourceCard(language: com.example.aiphysical.presentation.auth.AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(PsychCritical.copy(0.10f), PsychCritical.copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(PsychCritical.copy(0.45f), PsychCritical.copy(0.12f))), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🆘", fontSize = 20.sp)
            Text(language.pick("Экстренная психологическая помощь", "Emergency psychological support", "Шұғыл психологиялық көмек"), color = PsychCritical, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            language.pick(
                "Если тебе нужна срочная помощь — позвони на телефон доверия: 150 (бесплатно)",
                "If you need urgent help, call the helpline: 150 (free)",
                "Шұғыл көмек керек болса, сенім телефонына қоңырау шал: 150 (тегін)"
            ),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 18.sp
        )
    }
}

