package com.group2.movi.ui.tasks

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group2.movi.data.repository.ChatRepository
import com.group2.movi.data.repository.UserRepository
import com.group2.movi.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatComposerState(
    val draft: String = "",
    val attachmentUri: Uri? = null,
    val attachmentLabel: String = "Item photo",
    val sending: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val chatRepo: ChatRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedState.get<String>("taskId"))
    val currentUid: String? get() = userRepo.currentUid

    val messages: StateFlow<List<Message>> = chatRepo.observeMessages(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _composer = MutableStateFlow(ChatComposerState())
    val composer: StateFlow<ChatComposerState> = _composer.asStateFlow()

    fun setDraft(v: String) { _composer.value = _composer.value.copy(draft = v) }

    fun setAttachment(uri: Uri?) {
        _composer.value = _composer.value.copy(attachmentUri = uri)
    }

    fun setAttachmentLabel(label: String) {
        _composer.value = _composer.value.copy(attachmentLabel = label)
    }

    fun send() {
        val state = _composer.value
        val text = state.draft.trim()
        val attachment = state.attachmentUri
        val uid = currentUid ?: run {
            _composer.value = state.copy(error = "Please sign in first.")
            return
        }
        if ((text.isEmpty() && attachment == null) || state.sending) return

        _composer.value = state.copy(sending = true, error = null, success = null)
        viewModelScope.launch {
            val me = userRepo.getUser(uid)
            val imageUrl = attachment?.let { chatRepo.uploadChatImage(it).getOrElse { error ->
                _composer.value = _composer.value.copy(
                    sending = false,
                    error = friendlyError(error)
                )
                return@launch
            } }
            val result = chatRepo.sendMessage(
                taskId = taskId,
                senderId = uid,
                senderName = me?.displayName ?: "User",
                text = text,
                imageUrl = imageUrl,
                imageLabel = if (imageUrl != null) state.attachmentLabel else null
            )
            if (result.isFailure) {
                _composer.value = _composer.value.copy(
                    sending = false,
                    error = friendlyError(result.exceptionOrNull())
                )
            } else {
                _composer.value = ChatComposerState(success = "Message sent.")
            }
        }
    }

    private fun friendlyError(error: Throwable?): String {
        val message = error?.message.orEmpty()
        return when {
            message.contains("network", ignoreCase = true) ->
                "Network error. Message was not sent."
            else -> "Failed to send message. Please try again."
        }
    }
}
