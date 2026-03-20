package com.example.aiphysical.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.OrganizationCustomTestSessionState
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.ui.theme.PsychBackground
import com.example.aiphysical.ui.theme.PsychTeal
import com.example.aiphysical.ui.theme.TextHint
import com.example.aiphysical.ui.theme.TextPrimary
import com.example.aiphysical.ui.theme.TextSecondary

@Composable
fun StudentOrganizationCustomTestScreen(
	session: OrganizationCustomTestSessionState,
	language: AppLanguage = AppLanguage.RU,
	onClose: () -> Unit,
	onOptionSelected: (String) -> Unit,
	onNext: () -> Unit,
	onSubmit: () -> Unit,
) {
	val question = session.currentQuestion ?: return
	val progressLabel = "${question.order} / ${session.test.questions.size}"
	val isReady = session.selectedOptionId != null && !session.isSubmitting

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(PsychBackground.copy(alpha = 0.98f))
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 20.dp)
				.padding(top = 24.dp, bottom = 32.dp),
			verticalArrangement = Arrangement.spacedBy(18.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = language.pick("✕ Закрыть", "✕ Close", "✕ Жабу"),
					color = TextSecondary,
					fontSize = 14.sp,
					fontWeight = FontWeight.SemiBold,
					modifier = Modifier.clickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null,
						onClick = onClose
					)
				)
				Column(horizontalAlignment = Alignment.End) {
					Text(
						text = session.test.title,
						color = TextPrimary,
						fontSize = 22.sp,
						fontWeight = FontWeight.ExtraBold,
						textAlign = TextAlign.End
					)
					Text(language.pick("Вопрос $progressLabel", "Question $progressLabel", "$progressLabel сұрақ"), color = PsychTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
				}
			}

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.clip(RoundedCornerShape(20.dp))
					.background(Brush.horizontalGradient(listOf(PsychTeal.copy(0.15f), Color(0xFF9D5FF5).copy(0.10f))))
					.border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp))
					.padding(18.dp)
			) {
				Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
					Text(
						language.pick("ВЫБЕРИТЕ ОДИН ВАРИАНТ", "CHOOSE ONE OPTION", "БІР НҰСҚАНЫ ТАҢДАҢЫЗ"),
						color = TextHint,
						fontSize = 10.sp,
						fontWeight = FontWeight.ExtraBold,
						letterSpacing = 1.sp
					)
					Text(
						question.text,
						color = TextPrimary,
						fontSize = 20.sp,
						lineHeight = 28.sp,
						fontWeight = FontWeight.ExtraBold
					)
					Text(
						language.pick("Ответ будет отправлен психологу после завершения теста.", "Your answer will be sent to the psychologist after the test is completed.", "Жауап тест аяқталғаннан кейін психологқа жіберіледі."),
						color = TextSecondary,
						fontSize = 13.sp
					)
				}
			}

			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				question.options.sortedBy { it.order }.forEach { option ->
					val isSelected = session.selectedOptionId == option.id
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.clip(RoundedCornerShape(18.dp))
							.background(if (isSelected) PsychTeal.copy(0.18f) else Color.White.copy(0.05f))
							.border(
								1.dp,
								if (isSelected) PsychTeal.copy(0.45f) else Color.White.copy(0.08f),
								RoundedCornerShape(18.dp)
							)
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null,
								enabled = !session.isSubmitting,
								onClick = { onOptionSelected(option.id) }
							)
							.padding(16.dp)
					) {
						Row(
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							Box(
								modifier = Modifier
									.size(28.dp)
									.clip(RoundedCornerShape(10.dp))
									.background(if (isSelected) PsychTeal else Color.White.copy(0.08f))
									.border(
										1.dp,
										if (isSelected) PsychTeal else Color.White.copy(0.12f),
										RoundedCornerShape(10.dp)
									),
								contentAlignment = Alignment.Center
							) {
								Text(
									option.order.toString(),
									color = if (isSelected) Color.White else TextHint,
									fontWeight = FontWeight.Bold
								)
							}
							Text(option.text, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
						}
					}
				}
			}

			session.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.clip(RoundedCornerShape(14.dp))
						.background(Color(0xFFFF6B6B).copy(0.12f))
						.border(1.dp, Color(0xFFFF6B6B).copy(0.28f), RoundedCornerShape(14.dp))
						.padding(14.dp)
				) {
					Text(error, color = Color(0xFFFFA5A5), fontSize = 13.sp)
				}
			}

			HorizontalDivider(color = Color.White.copy(0.08f))

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.clip(RoundedCornerShape(18.dp))
					.background(
						if (isReady) Brush.horizontalGradient(listOf(PsychTeal.copy(0.95f), Color(0xFF6B7CFF).copy(0.85f)))
						else Brush.horizontalGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.08f)))
					)
					.clickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null,
						enabled = isReady,
						onClick = { if (session.isLastQuestion) onSubmit() else onNext() }
					)
					.padding(vertical = 16.dp),
				contentAlignment = Alignment.Center
			) {
				if (session.isSubmitting) {
					CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
				} else {
					Text(
						text = if (session.isLastQuestion) language.pick("Завершить тест", "Finish test", "Тестті аяқтау") else language.pick("Следующий вопрос", "Next question", "Келесі сұрақ"),
						color = if (isReady) Color.White else TextHint,
						fontSize = 16.sp,
						fontWeight = FontWeight.ExtraBold,
						textAlign = TextAlign.Center
					)
				}
			}

			Spacer(Modifier.height(24.dp))
		}
	}
}

