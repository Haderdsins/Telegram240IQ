package maxytkocorp.telegram240iq.dal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats_or_channels")
data class ChatEntity(
    @PrimaryKey val name: String,
    val owner: String,
    val isChannel: Boolean,
)