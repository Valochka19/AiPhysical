package com.example.aiphysical.presentation.chat

import com.example.aiphysical.data.model.ChatContactPreview
import com.example.aiphysical.data.model.PsychChatMessage

data class SupportChatUiState(
    val currentUserId: String = "",
    val currentUserRole: String = "",
    val currentUserName: String = "",
    val contacts: List<ChatContactPreview> = emptyList(),
    val selectedContact: ChatContactPreview? = null,
    val activeChatId: String = "",
    val messages: List<PsychChatMessage> = emptyList(),
    val input: String = "",
    val isLoadingContacts: Boolean = true,
    val isLoadingMessages: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

sealed class SupportChatEvent {
    object Reload : SupportChatEvent()
    data class SelectContact(val contact: ChatContactPreview) : SupportChatEvent()
    data class UpdateInput(val value: String) : SupportChatEvent()
    object SendMessage : SupportChatEvent()
    object DismissError : SupportChatEvent()
    object ClearSelection : SupportChatEvent()
}

