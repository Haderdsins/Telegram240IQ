package maxytkocorp.telegram240iq.Web

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import maxytkocorp.telegram240iq.Models.MessageData

class MessageDataAdapter {

    @FromJson
    fun fromJson(json: Map<String, Map<String, String>>): MessageData {
        val dataType = json.keys.firstOrNull()
            ?: throw IllegalArgumentException("Unknown MessageData type")

        return when (dataType) {
            "Text" -> {
                val textData = json["Text"] ?: throw IllegalArgumentException("Missing Text data")
                val text =
                    textData["text"] ?: throw IllegalArgumentException("Text field is missing")
                MessageData.Text(text)
            }

            "Image" -> {
                val imageData =
                    json["Image"] ?: throw IllegalArgumentException("Missing Image data")
                val link = imageData["link"] ?: ""
                MessageData.Image(link)
            }

            else -> throw IllegalArgumentException("Unknown MessageData type")
        }
    }

    @ToJson
    fun toJson(data: MessageData): Map<String, Map<String, String?>> {
        return when (data) {
            is MessageData.Text -> mapOf("Text" to mapOf("text" to data.text))
            is MessageData.Image -> mapOf("Image" to mapOf("link" to data.link))
        }
    }
}
