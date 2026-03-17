@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aiphysical.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.ChatMessage
import com.example.aiphysical.presentation.student.StudentEvent
import com.example.aiphysical.presentation.student.StudentUiState
import com.example.aiphysical.presentation.student.StudentViewModel
import com.example.aiphysical.ui.theme.*

// Token estimation constant (same as in ViewModel)
private const val MAX_TOKENS = 50_000
private const val CHARS_PER_TOKEN = 4

@Composable
fun StudentAiChatTab(
    state: StudentUiState,
    vm: StudentViewModel,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        ChatHeader(
            messageCount = state.chatMessages.size,
            onClear = { vm.onEvent(StudentEvent.ClearChatHistory) }
        )

        // ── Message list ──────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.chatMessages.isEmpty()) {
                item { ChatEmptyState() }
            }

            items(state.chatMessages) { msg ->
                ChatBubble(message = msg)
            }

            if (state.isChatLoading) {
                item { TypingIndicator() }
            }
        }

        // ── Error banner ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.chatError != null,
            enter = slideInVertically() + fadeIn(),
            exit  = slideOutVertically() + fadeOut()
        ) {
            state.chatError?.let { err ->
                ChatErrorBanner(
                    message = err,
                    onDismiss = { vm.onEvent(StudentEvent.ClearChatError) }
                )
            }
        }

        // ── Input field ───────────────────────────────────────────────────────
        ChatInputBar(
            value        = state.chatInput,
            isLoading    = state.isChatLoading,
            onValueChange = { vm.onEvent(StudentEvent.UpdateChatInput(it)) },
            onSend       = {
                if (state.chatInput.isNotBlank() && !state.isChatLoading) {
                    vm.onEvent(StudentEvent.SendChatMessage(state.chatInput))
                }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Chat Header
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatHeader(
    messageCount: Int,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF9D5FF5).copy(0.4f), PsychTeal.copy(0.2f))),
                    CircleShape
                )
                .border(1.dp, Brush.linearGradient(listOf(Color(0xFF9D5FF5), PsychTeal)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 20.sp)
        }

        Column(Modifier.weight(1f)) {
            Text(
                "Уми",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Gemini 1.5 Flash • $messageCount сообщ.",
                color = PsychTeal.copy(0.8f),
                fontSize = 11.sp
            )
        }

        if (messageCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.07f))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClear
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🗑 Очистить", color = Color.White.copy(0.6f), fontSize = 11.sp)
            }
        }
    }

    HorizontalDivider(
        color = Color(0xFF9D5FF5).copy(0.25f),
        thickness = 1.dp
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  Empty state
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🤖", fontSize = 56.sp)
        Text(
            "Уми готова помочь",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Задайте любой вопрос о здоровье,\nучёбе или психологическом благополучии",
            color = Color.White.copy(0.45f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(8.dp))

        // Suggestion chips
        val suggestions = listOf(
            "💡 Как снизить стресс?",
            "📚 Советы по обучению",
            "😴 Улучшить сон",
            "🧘 Медитация для новичков"
        )
        suggestions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { hint ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF9D5FF5).copy(0.1f))
                            .border(1.dp, Color(0xFF9D5FF5).copy(0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(hint, color = Color(0xFF9D5FF5).copy(0.9f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Chat Bubble
// ══════════════════════════════════════════════════════════════════════════════

@Composable
internal fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            // AI avatar
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        Brush.radialGradient(listOf(PsychTeal.copy(0.3f), Color(0xFF9D5FF5).copy(0.15f))),
                        CircleShape
                    )
                    .border(1.dp, PsychTeal.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 290.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) Brush.linearGradient(
                            listOf(Color(0xFF9D5FF5).copy(0.8f), PsychTeal.copy(0.6f))
                        ) else Brush.linearGradient(
                            listOf(
                                if (message.isError) Color(0xFFFF5370).copy(0.15f) else Color(0xFF1A1A35),
                                if (message.isError) Color(0xFFFF5370).copy(0.08f) else Color(0xFF141428)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (isUser) Brush.linearGradient(listOf(Color(0xFF9D5FF5), PsychTeal.copy(0.7f)))
                        else if (message.isError) Brush.linearGradient(listOf(Color(0xFFFF5370).copy(0.5f), Color(0xFFFF5370).copy(0.2f)))
                        else Brush.linearGradient(listOf(Color.White.copy(0.1f), Color.White.copy(0.04f))),
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text    = message.text,
                    color   = if (isUser) Color.White
                              else if (message.isError) Color(0xFFFF5370)
                              else Color.White.copy(0.9f),
                    fontSize   = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Text(
                text     = if (isUser) "Вы" else if (message.isError) "⚠️ Ошибка" else "Уми",
                color    = Color.White.copy(0.3f),
                fontSize = 10.sp
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            // User avatar
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF9D5FF5).copy(0.4f), Color(0xFF9D5FF5).copy(0.1f))),
                        CircleShape
                    )
                    .border(1.dp, Color(0xFF9D5FF5).copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 14.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Typing Indicator
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    Brush.radialGradient(listOf(PsychTeal.copy(0.3f), Color(0xFF9D5FF5).copy(0.15f))),
                    CircleShape
                )
                .border(1.dp, PsychTeal.copy(0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("🤖", fontSize = 14.sp) }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1A1A35))
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    val scale by infiniteTransition.animateFloat(
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
                            .scale(scale)
                            .background(PsychTeal.copy(0.8f), CircleShape)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Error Banner
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatErrorBanner(message: String, onDismiss: () -> Unit) {
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
        Text("⚠️", fontSize = 16.sp)
        Text(
            message,
            color    = Color(0xFFFF5370),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            lineHeight = 16.sp
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFFFF5370).copy(0.2f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) { Text("✕", color = Color(0xFFFF5370), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Input Bar
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatInputBar(
    value: String,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val estimatedTokens = value.length / CHARS_PER_TOKEN
    val nearLimit       = estimatedTokens > MAX_TOKENS - 5_000   // warn at 45k
    val overLimit       = estimatedTokens > MAX_TOKENS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050010).copy(0.95f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Token counter (visible only when > 10k tokens typed)
        if (estimatedTokens > 10_000) {
            Text(
                "~$estimatedTokens / 50 000 токенов",
                color    = if (overLimit) Color(0xFFFF5370) else if (nearLimit) Color(0xFFFFB800) else PsychTeal,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value    = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder  = {
                    Text("Задайте вопрос Уми...", color = Color.White.copy(0.35f), fontSize = 14.sp)
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines  = 5,
                isError   = overLimit,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = if (overLimit) Color(0xFFFF5370) else PsychTeal.copy(0.7f),
                    unfocusedBorderColor = Color.White.copy(0.15f),
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = PsychTeal,
                    focusedContainerColor   = Color(0xFF0D0D22),
                    unfocusedContainerColor = Color(0xFF0D0D22),
                    errorBorderColor        = Color(0xFFFF5370),
                    errorContainerColor     = Color(0xFFFF5370).copy(0.05f)
                ),
                shape = RoundedCornerShape(16.dp)
            )

            // Send button
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                        enabled   = !isLoading && value.isNotBlank() && !overLimit,
                        onClick   = onSend
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color    = PsychTeal,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "➤",
                        fontSize   = 20.sp,
                        color      = if (value.isNotBlank() && !overLimit) Color.White else Color.White.copy(0.25f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

