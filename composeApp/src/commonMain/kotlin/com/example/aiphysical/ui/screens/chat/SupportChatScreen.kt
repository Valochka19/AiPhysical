package com.example.aiphysical.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.ChatContactPreview
import com.example.aiphysical.data.model.PsychChatMessage
import com.example.aiphysical.presentation.chat.SupportChatUiState
import com.example.aiphysical.ui.theme.AlertOrange
import com.example.aiphysical.ui.theme.GlassBorder
import com.example.aiphysical.ui.theme.PsychBackground
import com.example.aiphysical.ui.theme.PsychCritical
import com.example.aiphysical.ui.theme.PsychTeal
import com.example.aiphysical.ui.theme.TextHint
import com.example.aiphysical.ui.theme.TextPrimary
import com.example.aiphysical.ui.theme.TextSecondary
import com.example.aiphysical.ui.theme.VioletGlow

enum class DashboardOverlayDestination { None, Points, Chat }

@Composable
fun DashboardDrawerSheet(
    onProfileClick: () -> Unit,
    onPointsClick: () -> Unit,
    onChatClick: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF14121F),
        drawerContentColor = TextPrimary,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Меню", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Быстрый переход к профилю, баллам и мессенджеру поддержки",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            HorizontalDivider(color = Color.White.copy(0.08f))
            DrawerSheetItem("👤", "Личный кабинет", "Открыть профиль", onProfileClick)
            DrawerSheetItem("⭐", "Баллы", "Раздел в разработке", onPointsClick)
            DrawerSheetItem("💬", "Чат", "Диалоги с психологом", onChatClick, accent = PsychTeal)
        }
    }
}

@Composable
fun DashboardMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.08f))
            .border(1.dp, Color.White.copy(0.16f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("☰", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PointsPlaceholderScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0914), Color(0xFF14121F), Color(0xFF0C0A16))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        OverlayHeader(title = title, subtitle = "Раздел пока работает как заглушка", onBack = onBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(AlertOrange.copy(0.12f), Color.White.copy(0.03f))))
                .border(1.dp, AlertOrange.copy(0.24f), RoundedCornerShape(26.dp))
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Баллы", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Сюда позже можно добавить рейтинг, достижения, бонусы и историю начислений. Сейчас экран оставлен как аккуратная заглушка.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
fun SupportChatScreen(
    state: SupportChatUiState,
    onBack: () -> Unit,
    onContactSelected: (ChatContactPreview) -> Unit,
    onConversationBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.selectedContact?.uid) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PsychBackground, Color(0xFF141220), Color(0xFF0A0912))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        OverlayHeader(
            title = "Чат с психологом",
            subtitle = when {
                state.currentUserRole == "psychologist" -> "Выберите студента или преподавателя и продолжайте диалог в реальном времени"
                else -> "Выберите психолога своей организации и начните личный диалог"
            },
            onBack = onBack
        )

        Spacer(Modifier.height(14.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 880.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SupportContactsPane(
                        state = state,
                        onContactSelected = onContactSelected,
                        modifier = Modifier.width(320.dp)
                    )
                    SupportConversationPane(
                        state = state,
                        listStateKey = listState,
                        onConversationBack = onConversationBack,
                        onInputChange = onInputChange,
                        onSend = onSend,
                        onDismissError = onDismissError,
                        showBackToContacts = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                if (state.selectedContact == null) {
                    SupportContactsPane(
                        state = state,
                        onContactSelected = onContactSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SupportConversationPane(
                        state = state,
                        listStateKey = listState,
                        onConversationBack = onConversationBack,
                        onInputChange = onInputChange,
                        onSend = onSend,
                        onDismissError = onDismissError,
                        showBackToContacts = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(0.08f))
                .border(1.dp, Color.White.copy(0.16f), RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("Назад", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DrawerSheetItem(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accent: Color = VioletGlow,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(accent.copy(0.10f), Color.White.copy(0.03f))))
            .border(1.dp, accent.copy(0.20f), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accent.copy(0.18f), RoundedCornerShape(14.dp))
                .border(1.dp, accent.copy(0.25f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Text("›", color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SupportContactsPane(
    state: SupportChatUiState,
    onContactSelected: (ChatContactPreview) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        when {
            state.isLoadingContacts -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PsychTeal, strokeWidth = 2.dp)
                }
            }
            state.contacts.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    EmptyStateMessage(
                        title = if (state.currentUserRole == "psychologist") "Пока нет доступных диалогов" else "Психологов пока нет",
                        message = if (state.currentUserRole == "psychologist") {
                            "Когда в организации появятся студенты или преподаватели, они отобразятся в этом списке."
                        } else {
                            "В вашей организации пока нет доступных психологов. Попробуйте позже или обратитесь к администратору."
                        }
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.contacts, key = { it.uid }) { contact ->
                        ContactRow(
                            contact = contact,
                            isSelected = state.selectedContact?.uid == contact.uid,
                            onClick = { onContactSelected(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ChatContactPreview,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accent = when (contact.role.lowercase()) {
        "psychologist" -> PsychTeal
        "teacher" -> AlertOrange
        else -> VioletGlow
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) Brush.horizontalGradient(listOf(accent.copy(0.16f), Color.White.copy(0.04f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(0.05f), Color.White.copy(0.03f)))
            )
            .border(1.dp, if (isSelected) accent.copy(0.45f) else Color.White.copy(0.10f), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarBubble(name = contact.fullName, accent = accent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = contact.fullName.ifBlank { contact.email },
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (contact.hasExistingChat) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(0.18f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("диалог", color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Text(
                text = when (contact.role.lowercase()) {
                    "psychologist" -> "Психолог"
                    "teacher" -> "Преподаватель"
                    else -> "Студент"
                },
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = contact.lastMessageText.ifBlank { "Открыть диалог" },
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SupportConversationPane(
    state: SupportChatUiState,
    listStateKey: androidx.compose.foundation.lazy.LazyListState,
    onConversationBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissError: () -> Unit,
    showBackToContacts: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        val selectedContact = state.selectedContact
        if (selectedContact == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                EmptyStateMessage(
                    title = "Выберите контакт",
                    message = "Слева отображаются доступные собеседники. Выберите психолога, студента или преподавателя, чтобы открыть историю сообщений."
                )
            }
            return
        }

        ConversationHeader(
            contact = selectedContact,
            showBackToContacts = showBackToContacts,
            onConversationBack = onConversationBack
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoadingMessages -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PsychTeal, strokeWidth = 2.dp)
                    }
                }
                state.messages.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        EmptyStateMessage(
                            title = "Сообщений пока нет",
                            message = "Напишите первое сообщение — диалог создастся автоматически и сразу синхронизируется у обеих сторон."
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listStateKey,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.messages, key = { it.messageId }) { message ->
                            MessageBubble(
                                message = message,
                                isOwn = message.senderId == state.currentUserId
                            )
                        }
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            ErrorBanner(errorMessage = state.errorMessage, onDismiss = onDismissError)
        }

        SupportMessageInput(
            value = state.input,
            isSending = state.isSending,
            onValueChange = onInputChange,
            onSend = onSend
        )
    }
}

@Composable
private fun ConversationHeader(
    contact: ChatContactPreview,
    showBackToContacts: Boolean,
    onConversationBack: () -> Unit,
) {
    val accent = when (contact.role.lowercase()) {
        "psychologist" -> PsychTeal
        "teacher" -> AlertOrange
        else -> VioletGlow
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showBackToContacts) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(0.06f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onConversationBack
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text("←", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        AvatarBubble(name = contact.fullName, accent = accent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(contact.fullName.ifBlank { contact.email }, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                when (contact.role.lowercase()) {
                    "psychologist" -> "Психолог организации"
                    "teacher" -> "Преподаватель"
                    else -> "Студент"
                },
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    HorizontalDivider(color = Color.White.copy(0.08f))
}

@Composable
private fun MessageBubble(
    message: PsychChatMessage,
    isOwn: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).clip(RoundedCornerShape(18.dp)).background(
                if (isOwn) Brush.horizontalGradient(listOf(VioletGlow.copy(0.85f), PsychTeal.copy(0.72f)))
                else Brush.horizontalGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.05f)))
            ).border(
                1.dp,
                if (isOwn) PsychTeal.copy(0.30f) else Color.White.copy(0.08f),
                RoundedCornerShape(18.dp)
            ).padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                text = message.text,
                color = if (isOwn) Color.White else TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SupportMessageInput(
    value: String,
    isSending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars
                    .union(WindowInsets.ime)
                    .only(WindowInsetsSides.Bottom)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Введите сообщение...", color = Color.White.copy(0.35f), fontSize = 14.sp) },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PsychTeal.copy(0.7f),
                    unfocusedBorderColor = Color.White.copy(0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF10101E),
                    unfocusedContainerColor = Color(0xFF10101E),
                    cursorColor = PsychTeal
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (value.isNotBlank() && !isSending) Brush.linearGradient(listOf(VioletGlow, PsychTeal))
                        else Brush.linearGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.05f)))
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = value.isNotBlank() && !isSending,
                        onClick = onSend
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("➤", color = if (value.isNotBlank()) Color.White else Color.White.copy(0.25f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            "Сообщения синхронизируются в реальном времени. Диалог видят только его участники.",
            color = TextHint,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ErrorBanner(
    errorMessage: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PsychCritical.copy(0.12f))
            .border(1.dp, PsychCritical.copy(0.32f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠️", fontSize = 14.sp)
        Text(errorMessage, color = PsychCritical, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(PsychCritical.copy(0.18f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("✕", color = PsychCritical, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyStateMessage(
    title: String,
    message: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("💬", fontSize = 42.sp)
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(message, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun AvatarBubble(name: String, accent: Color) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(Brush.radialGradient(listOf(accent.copy(0.40f), accent.copy(0.08f))), CircleShape)
            .border(1.dp, accent.copy(0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "?" },
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

