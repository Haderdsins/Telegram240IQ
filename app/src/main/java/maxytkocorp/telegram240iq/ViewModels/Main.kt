package maxytkocorp.telegram240iq.ViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import maxytkocorp.telegram240iq.ChatRepository
import maxytkocorp.telegram240iq.Models.Chat
import maxytkocorp.telegram240iq.Models.Message
import maxytkocorp.telegram240iq.Models.MessageData
import maxytkocorp.telegram240iq.Web.RetrofitInstance
import maxytkocorp.telegram240iq.Web.SessionManager

class Main(
    private val sessionManager: SessionManager,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<MainScreenState>(MainScreenState.Loading)
    val state = _state.asStateFlow()

    private val _selectedChat = MutableStateFlow<Chat?>(null)
    val selectedChatOrChannel = _selectedChat.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun loadChats() {
        viewModelScope.launch {
            try {
                val token = sessionManager.token.firstOrNull().orEmpty()
                val username = sessionManager.username.firstOrNull().orEmpty()

                if (token.isEmpty() || username.isEmpty()) {
                    _state.value = MainScreenState.Error("Token or username is missing")
                    return@launch
                }

                val chatsAndChannels = chatRepository.getChats(username, token) {
                    logout()
                }
                _state.value = MainScreenState.Success(chatsAndChannels)
            } catch (e: Exception) {
                _state.value = MainScreenState.Error("Failed to load chats: ${e.message}")
            }
        }
    }

    private fun loadMessagesForChat(chat: Chat) {
        viewModelScope.launch {
            try {
                val token = sessionManager.token.firstOrNull().orEmpty()
                val username = sessionManager.username.firstOrNull().orEmpty()

                if (token.isEmpty() || username.isEmpty()) {
                    _state.value = MainScreenState.Error("Token or username is missing")
                    return@launch
                }

                val messages = chatRepository.getMessagesForChat(username, chat, token) {
                    logout()
                }

                _messages.value = messages
                chat.lastKnownId = messages.minOfOrNull { it.id } ?: chat.lastKnownId
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load messages: ${e.message}")
                _messages.value = emptyList()
            }
        }
    }

    fun selectChat(item: Chat?) {
        _selectedChat.value = item
        if (item != null) {
            loadMessagesForChat(item)
        } else {
            _messages.value = emptyList()
        }
    }

    fun sendMessage(chatId: String, text: String) {
        viewModelScope.launch {
            try {
                val token = sessionManager.token.firstOrNull()
                val username = sessionManager.username.firstOrNull()

                if (token.isNullOrEmpty() || username.isNullOrEmpty()) {
                    _state.value = MainScreenState.Error("Token or username is missing")
                    return@launch
                }

                val result = chatRepository.sendMessage(
                    from = username,
                    to = chatId,
                    data = MessageData.Text(text),
                    token = token
                ) {
                    logout()
                }

                if (result.isSuccess) {
                    selectedChatOrChannel.value?.let { loadMessagesForChat(it) }
                } else {
                    Log.e(
                        "MainViewModel",
                        "Failed to send message: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error sending message: ${e.message}")
            }
        }
    }

    suspend fun loadMoreMessages(chat: Chat): Boolean {
        return try {
            val token = sessionManager.token.firstOrNull().orEmpty()
            val username = sessionManager.username.firstOrNull().orEmpty()

            if (token.isEmpty()) {
                _state.value = MainScreenState.Error("Token is missing")
                return false
            }

            Log.d("MainViewModel", "Loading more messages for chat: ${chat.lastKnownId}")
            val newMessages = chatRepository.getMessagesForChat(
                username = username,
                channel = chat,
                token = token,
                lastKnownId = chat.lastKnownId,
                rev = true
            ) {
                logout()
            }

            if (newMessages.isNotEmpty()) {
                _messages.value += newMessages
                chat.lastKnownId = newMessages.minOfOrNull { it.id } ?: chat.lastKnownId
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to load more messages: ${e.message}")
            false
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val token = sessionManager.token.firstOrNull()

                if (!token.isNullOrEmpty()) {
                    val response = RetrofitInstance.apiService.logoutUser(token)

                    if (response.isSuccessful) {
                        Log.d("MainViewModel", "Logout successful")
                    } else {
                        Log.d("MainViewModel", "Logout failed: ${response.message()}")
                    }
                }

                sessionManager.clearSession()
                _state.value = MainScreenState.Loading
            } catch (e: Exception) {
                Log.d("MainViewModel", "Logout error: ${e.message}")
            }
        }
    }

    sealed class MainScreenState {
        object Loading : MainScreenState()
        data class Success(val chatsAndChannels: List<Chat>) : MainScreenState()
        data class Error(val message: String) : MainScreenState()
    }
}
