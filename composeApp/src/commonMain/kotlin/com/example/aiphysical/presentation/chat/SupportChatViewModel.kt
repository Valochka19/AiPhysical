package com.example.aiphysical.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aiphysical.data.model.ChatContactPreview
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.data.model.buildPsychChatId
import com.example.aiphysical.data.model.isAllowedPsychChatPair
import com.example.aiphysical.data.model.normalizedPsychChatRole
import com.example.aiphysical.data.model.supportMessageMaxLength
import com.example.aiphysical.data.model.trimmedSupportMessage
import com.example.aiphysical.data.service.FirestoreResult
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.auth.pick
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class SupportChatViewModel(
    private val uid: String,
    private val orgId: String,
    private val firestoreService: FirestoreService,
    initialLanguage: AppLanguage = AppLanguage.RU,
) : ViewModel() {

    private val _state = MutableStateFlow(SupportChatUiState(currentLanguage = initialLanguage))
    val state: StateFlow<SupportChatUiState> = _state.asStateFlow()

    private var currentRole: String = ""
    private var contactsCache: List<UserProfile> = emptyList()
    private var threadsJob: Job? = null
    private var messagesJob: Job? = null
    private var threadCache: Map<String, com.example.aiphysical.data.model.PsychChatThread> = emptyMap()
    private var isActive: Boolean = false

    fun onEvent(event: SupportChatEvent) {
        when (event) {
            SupportChatEvent.Reload -> reload()
            is SupportChatEvent.ChangeLanguage -> _state.update { it.copy(currentLanguage = event.language) }
            is SupportChatEvent.SelectContact -> selectContact(event.contact)
            is SupportChatEvent.UpdateInput -> _state.update { it.copy(input = event.value) }
            SupportChatEvent.SendMessage -> sendMessage()
            SupportChatEvent.DismissError -> _state.update { it.copy(errorMessage = null) }
            SupportChatEvent.ClearSelection -> {
                messagesJob?.cancel()
                _state.update {
                    it.copy(
                        selectedContact = null,
                        activeChatId = "",
                        messages = emptyList(),
                        isLoadingMessages = false,
                        input = "",
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun setActive(active: Boolean) {
        if (isActive == active) return
        isActive = active

        if (!active) {
            stopObserving()
            _state.update {
                it.copy(
                    isLoadingContacts = false,
                    isLoadingMessages = false,
                    isSending = false,
                    errorMessage = null
                )
            }
            return
        }

        if (_state.value.currentUserId.isBlank()) {
            reload()
            return
        }

        _state.update { it.copy(errorMessage = null) }
        observeThreads()
        resumeSelectedConversation()
    }

    private fun reload() {
        _state.update { it.copy(isLoadingContacts = true, errorMessage = null) }
        stopObserving()
        viewModelScope.launch {
            val profileResult = firestoreService.getUserProfile(uid)
            val profile = (profileResult as? FirestoreResult.UserProfileSuccess)?.profile
            if (profile == null) {
                _state.update {
                    it.copy(
                        isLoadingContacts = false,
                                errorMessage = (profileResult as? FirestoreResult.Failure)?.message
                                    ?: _state.value.currentLanguage.pick(
                                        ru = "Не удалось загрузить профиль",
                                        en = "Failed to load profile",
                                        kz = "Профильді жүктеу мүмкін болмады"
                                    )
                    )
                }
                return@launch
            }

            currentRole = profile.role.normalizedPsychChatRole()
            _state.update {
                it.copy(
                    currentUserId = uid,
                    currentUserRole = currentRole,
                    currentUserName = profile.fullName
                )
            }

            val contactsResult = firestoreService.getPsychChatContacts(orgId, uid, currentRole)
            contactsCache = when (contactsResult) {
                is FirestoreResult.ChatContactsSuccess -> contactsResult.contacts
                else -> emptyList()
            }
            _state.update {
                it.copy(
                    isLoadingContacts = false,
                    errorMessage = (contactsResult as? FirestoreResult.Failure)?.message
                )
            }
            rebuildContacts()
            if (isActive) {
                observeThreads()
                resumeSelectedConversation()
            }
        }
    }

    private fun observeThreads() {
        if (!isActive) return
        threadsJob?.cancel()
        threadsJob = viewModelScope.launch {
            firestoreService.observePsychChatThreads(orgId, uid)
                .catch { error -> _state.update { it.copy(errorMessage = error.message, isLoadingContacts = false) } }
                .collect { result ->
                    when (result) {
                        is FirestoreResult.PsychChatThreadsSuccess -> {
                            threadCache = result.threads.associateBy { it.chatId }
                            rebuildContacts()
                        }
                        is FirestoreResult.Failure -> _state.update { it.copy(errorMessage = result.message, isLoadingContacts = false) }
                        else -> Unit
                    }
                }
        }
    }

    private fun stopObserving() {
        threadsJob?.cancel()
        threadsJob = null
        messagesJob?.cancel()
        messagesJob = null
    }

    private fun rebuildContacts() {
        val mergedContacts = contactsCache
            .map { profile ->
                val chatId = buildPsychChatId(orgId, uid, profile.uid)
                val thread = threadCache[chatId]
                ChatContactPreview(
                    uid = profile.uid,
                    fullName = profile.fullName,
                    email = profile.email,
                    role = profile.role,
                    orgId = profile.orgId,
                    chatId = chatId,
                    hasExistingChat = thread != null,
                    lastMessageText = thread?.lastMessageText.orEmpty(),
                    lastMessageAt = thread?.lastMessageAt ?: 0L,
                )
            }
            .sortedWith(
                compareByDescending<ChatContactPreview> { it.hasExistingChat }
                    .thenByDescending { it.lastMessageAt }
                    .thenBy { it.fullName.lowercase() }
            )

        val currentState = _state.value
        val selectedUid = currentState.selectedContact?.uid
        val resolvedSelected = mergedContacts.firstOrNull { it.uid == selectedUid } ?: currentState.selectedContact
        if (currentState.contacts == mergedContacts && currentState.selectedContact == resolvedSelected) {
            return
        }
        _state.update { state ->
            state.copy(
                contacts = mergedContacts,
                selectedContact = resolvedSelected
            )
        }
    }

    private fun selectContact(contact: ChatContactPreview) {
        if (!isAllowedPsychChatPair(currentRole, contact.role)) {
            _state.update { it.copy(errorMessage = unsupportedChatMessage(it.currentLanguage)) }
            return
        }
        val chatId = buildPsychChatId(orgId, uid, contact.uid)
        if (_state.value.activeChatId == chatId && _state.value.selectedContact?.uid == contact.uid) {
            return
        }
        _state.update {
            it.copy(
                selectedContact = contact.copy(chatId = chatId),
                activeChatId = chatId,
                messages = emptyList(),
                isLoadingMessages = true,
                input = "",
                errorMessage = null
            )
        }
        observeMessages(chatId, showLoading = true)
    }

    private fun observeMessages(chatId: String, showLoading: Boolean) {
        if (!isActive) return
        messagesJob?.cancel()
        if (showLoading) {
            _state.update { it.copy(isLoadingMessages = true) }
        }
        messagesJob = viewModelScope.launch {
            firestoreService.observePsychChatMessages(chatId)
                .catch { error -> _state.update { it.copy(isLoadingMessages = false, errorMessage = error.message) } }
                .collect { result ->
                    when (result) {
                        is FirestoreResult.PsychChatMessagesSuccess -> {
                            val sortedMessages = result.messages.sortedBy { message -> message.createdAt }
                            _state.update {
                                if (it.messages == sortedMessages && !it.isLoadingMessages) {
                                    it
                                } else {
                                    it.copy(
                                        messages = sortedMessages,
                                        isLoadingMessages = false
                                    )
                                }
                            }
                        }
                        is FirestoreResult.Failure -> _state.update { it.copy(isLoadingMessages = false, errorMessage = result.message) }
                        else -> Unit
                    }
                }
        }
    }

    private fun resumeSelectedConversation() {
        val chatId = _state.value.activeChatId
        if (chatId.isBlank()) return
        observeMessages(chatId, showLoading = _state.value.messages.isEmpty())
    }

    private fun sendMessage() {
        val selectedContact = _state.value.selectedContact ?: run {
            _state.update { it.copy(errorMessage = it.currentLanguage.pick(
                ru = "Сначала выберите собеседника",
                en = "Choose a contact first",
                kz = "Алдымен сұхбаттасушыны таңдаңыз"
            )) }
            return
        }
        val rawMessage = _state.value.input
        val trimmedMessage = rawMessage.trimmedSupportMessage(supportMessageMaxLength())
        if (trimmedMessage.isBlank()) {
            _state.update { it.copy(errorMessage = it.currentLanguage.pick(
                ru = "Нельзя отправить пустое сообщение",
                en = "You cannot send an empty message",
                kz = "Бос хабарламаны жіберу мүмкін емес"
            )) }
            return
        }
        if (rawMessage.trim().length > supportMessageMaxLength()) {
            _state.update {
                it.copy(errorMessage = it.currentLanguage.pick(
                    ru = "Сообщение слишком длинное. Максимум — ${supportMessageMaxLength()} символов",
                    en = "Message is too long. Maximum — ${supportMessageMaxLength()} characters",
                    kz = "Хабарлама тым ұзын. Шегі — ${supportMessageMaxLength()} таңба"
                ))
            }
            return
        }
        if (!isAllowedPsychChatPair(currentRole, selectedContact.role)) {
            _state.update { it.copy(errorMessage = unsupportedChatMessage(it.currentLanguage)) }
            return
        }

        _state.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            val result = firestoreService.sendPsychChatMessage(
                orgId = orgId,
                senderId = uid,
                senderRole = currentRole,
                recipientId = selectedContact.uid,
                recipientRole = selectedContact.role,
                text = trimmedMessage,
            )
            when (result) {
                is FirestoreResult.GenericSuccess -> _state.update { it.copy(input = "", isSending = false) }
                is FirestoreResult.Failure -> _state.update { it.copy(isSending = false, errorMessage = result.message) }
                else -> _state.update { it.copy(isSending = false) }
            }
        }
    }

    override fun onCleared() {
        stopObserving()
        super.onCleared()
    }

    companion object {
        fun factory(
            uid: String,
            orgId: String,
            firestoreService: FirestoreService,
            initialLanguage: AppLanguage = AppLanguage.RU,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                SupportChatViewModel(uid, orgId, firestoreService, initialLanguage) as T
        }
    }

    private fun unsupportedChatMessage(language: AppLanguage): String = language.pick(
        ru = "Этот чат недоступен для выбранной роли",
        en = "This chat is not available for the selected role",
        kz = "Бұл чат таңдалған рөл үшін қолжетімсіз"
    )
}

