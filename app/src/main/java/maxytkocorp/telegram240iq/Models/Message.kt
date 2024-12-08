package maxytkocorp.telegram240iq.Models

import com.squareup.moshi.JsonClass
import maxytkocorp.telegram240iq.dal.MessageEntity

@JsonClass(generateAdapter = true)
data class Message(
    val id: Int,
    val from: String,
    val to: String,
    val data: MessageData,
    val time: String?,
) {
    fun toEntity(): MessageEntity = MessageEntity(
        id = id,
        from = from,
        to = to,
        text = extractText(data),
        imageLink = extractImageLink(data),
        time = time.orEmpty()
    )

    private fun extractText(data: MessageData): String? =
        (data as? MessageData.Text)?.text

    private fun extractImageLink(data: MessageData): String? =
        (data as? MessageData.Image)?.link
}

@JsonClass(generateAdapter = true)
data class MessageRequest(
    val from: String,
    val to: String,
    val data: MessageData,
)

@JsonClass(generateAdapter = true)
sealed class MessageData {
    data class Text(val text: String) : MessageData()
    data class Image(val link: String?) : MessageData()
}
