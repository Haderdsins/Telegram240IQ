package maxytkocorp.telegram240iq

import android.util.Log
import maxytkocorp.telegram240iq.Models.Chat
import maxytkocorp.telegram240iq.Models.Message
import maxytkocorp.telegram240iq.Models.MessageData
import maxytkocorp.telegram240iq.Models.MessageRequest
import maxytkocorp.telegram240iq.Web.ApiService
import maxytkocorp.telegram240iq.dal.ChatDao
import maxytkocorp.telegram240iq.dal.ChatEntity
import maxytkocorp.telegram240iq.dal.MessageDao

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val apiService: ApiService,
) {
    suspend fun getMessagesForChat(
        username: String,
        channel: Chat,
        token: String,
        lastKnownId: Int = Int.MAX_VALUE,
        limit: Int = 20,
        rev: Boolean = true,
        onUnauthorized: suspend () -> Unit,
    ): List<Message> {
        val allMessages = mutableListOf<Message>()
        var currentLastKnownId = lastKnownId

        try {
            while (true) {
                val messagesResponse = if (channel.isChannel) {
                    apiService.getChannelMessages(
                        channel.name, token, limit, currentLastKnownId, rev
                    )
                } else {
                    apiService.getInbox(username, token, limit, currentLastKnownId, rev)
                }

                if (!messagesResponse.isSuccessful) {
                    if (messagesResponse.code() == 401) {
                        onUnauthorized()
                        throw Exception("Unauthorized: Token expired or invalid")
                    }
                    throw Exception("Failed to fetch messages from network")
                }

                val fetchedMessages = messagesResponse.body() ?: emptyList()

                if (fetchedMessages.isEmpty()) break

                val filteredMessages = fetchedMessages.filter { message ->
                    message.from == channel.name || message.to == channel.name
                }

                allMessages.addAll(filteredMessages)

                currentLastKnownId = fetchedMessages.minOfOrNull { it.id } ?: 0

                updateLocalDatabase(filteredMessages)

                if (allMessages.size >= limit) break
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching messages: ${e.message}")
            return messageDao.getMessagesByNameWithLimit(
                channel.name, lastKnownId = currentLastKnownId, limit = limit
            ).map { entity ->
                val messageData = when {
                    !entity.text.isNullOrEmpty() -> MessageData.Text(entity.text)
                    !entity.imageLink.isNullOrEmpty() -> MessageData.Image(entity.imageLink)
                    else -> throw IllegalArgumentException("MessageEntity must have either text or imageLink")
                }

                Message(
                    id = entity.id,
                    from = entity.from,
                    to = entity.to,
                    data = messageData,
                    time = entity.time
                )
            }
        }

        return allMessages
    }

    private suspend fun updateLocalDatabase(messages: List<Message>) {
        messageDao.insertMessages(messages.map { it.toEntity() })
    }

    suspend fun getChats(
        username: String,
        token: String,
        onUnauthorized: suspend () -> Unit,
    ): List<Chat> {
        return try {
            val channelsResponse = apiService.getChannels(token)

            if (!channelsResponse.isSuccessful) {
                if (channelsResponse.code() == 401) {
                    onUnauthorized()
                    throw Exception("Unauthorized: Token expired or invalid")
                }
                throw Exception("Failed to fetch channels from network: ${channelsResponse.message()}")
            }

            val channels = channelsResponse.body() ?: emptyList()

            val uniqueChats = mutableSetOf<String>()
            val chats = mutableListOf<Chat>()

            var lastKnownId = Int.MAX_VALUE
            val pageSize = 100

            while (true) {
                val messagesResponse = apiService.getInbox(
                    username = username,
                    token = token,
                    lastKnownId = lastKnownId,
                    limit = pageSize,
                    reverse = true
                )

                if (!messagesResponse.isSuccessful) {
                    if (messagesResponse.code() == 401) {
                        onUnauthorized()
                        throw Exception("Unauthorized: Token expired or invalid")
                    }
                    throw Exception("Failed to fetch inbox messages from network: ${messagesResponse.message()}")
                }

                val fetchedMessages = messagesResponse.body() ?: emptyList()

                if (fetchedMessages.isEmpty()) break

                val filteredMessages =
                    fetchedMessages.filter { it.to != null && !it.to!!.contains("@channel") }

                filteredMessages.forEach { message ->
                    val otherParty = if (message.from == username) message.to!! else message.from
                    if (otherParty !in uniqueChats) {
                        uniqueChats.add(otherParty)
                        chats.add(Chat(otherParty, isChannel = false))
                    }
                }

                lastKnownId = fetchedMessages.minOfOrNull { it.id } ?: 0
            }

            val allItems = chats + channels.map {
                Chat(it, isChannel = true)
            }

            updateLocalChats(allItems, username)

            return allItems
        } catch (e: Exception) {
            if (e.message?.contains("Unauthorized") == true) {
                throw e
            }

            val localChats = chatDao.getTelegram(username)

            return localChats.map { Chat(it.name, isChannel = true) }
        }
    }

    private suspend fun updateLocalChats(channels: List<Chat>, username: String) {
        val chatEntities = channels.map {
            ChatEntity(name = it.name, owner = username, isChannel = it.isChannel)
        }
        chatDao.insertChatsOrChannels(chatEntities)
    }

    suspend fun sendMessage(
        from: String,
        to: String,
        data: MessageData,
        token: String,
        onUnauthorized: suspend () -> Unit,
    ): Result<Unit> {
        return try {
            val messageRequest = MessageRequest(from = from, to = to, data = data)
            val response = apiService.sendMessage(token, messageRequest)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                if (response.code() == 401) {
                    onUnauthorized()
                    throw Exception("Unauthorized: Token expired or invalid")
                }
                Result.failure(Exception("Failed to send message: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
