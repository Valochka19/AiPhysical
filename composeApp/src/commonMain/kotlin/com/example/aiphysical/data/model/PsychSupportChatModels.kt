package com.example.aiphysical.data.model

private const val DEFAULT_MAX_SUPPORT_MESSAGE_LENGTH = 4_000

data class PsychChatThread(
    val chatId: String = "",
    val orgId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantRoles: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class PsychChatMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderRole: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
)

data class ChatContactPreview(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val orgId: String = "",
    val chatId: String = "",
    val hasExistingChat: Boolean = false,
    val lastMessageText: String = "",
    val lastMessageAt: Long = 0L,
)

fun buildPsychChatId(orgId: String, uidA: String, uidB: String): String {
    val cleanOrgId = orgId.trim().replace("/", "_")
    val participants = listOf(uidA.trim(), uidB.trim())
        .map { it.replace("/", "_") }
        .sorted()
    return listOf(cleanOrgId, participants[0], participants[1]).joinToString("__")
}

fun isAllowedPsychChatPair(roleA: String, roleB: String): Boolean {
    val first = roleA.normalizedPsychChatRole()
    val second = roleB.normalizedPsychChatRole()
    return (first == "psychologist" && second in setOf("user", "teacher")) ||
        (second == "psychologist" && first in setOf("user", "teacher"))
}

fun String.normalizedPsychChatRole(): String = trim().lowercase()

fun String.trimmedSupportMessage(maxLength: Int = DEFAULT_MAX_SUPPORT_MESSAGE_LENGTH): String =
    trim().take(maxLength)

fun supportMessageMaxLength(): Int = DEFAULT_MAX_SUPPORT_MESSAGE_LENGTH

fun PsychChatThread.otherParticipantId(currentUid: String): String =
    participantIds.firstOrNull { it != currentUid }.orEmpty()

