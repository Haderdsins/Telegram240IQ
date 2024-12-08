package maxytkocorp.telegram240iq.dal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatDao {
    @Query(
        """
        SELECT * 
        FROM chats_or_channels
    """
    )
    suspend fun getAllChatsOrChannels(): List<ChatEntity>

    @Query(
        """
        SELECT * 
        FROM chats_or_channels 
        WHERE owner = :username
    """
    )
    suspend fun getTelegram(username: String): List<ChatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatsOrChannels(chats: List<ChatEntity>)
}
