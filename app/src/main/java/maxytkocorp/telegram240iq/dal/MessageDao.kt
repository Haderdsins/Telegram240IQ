package maxytkocorp.telegram240iq.dal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Query(
        """
        SELECT * FROM messages 
        WHERE [from] = :name OR [to] = :name 
        ORDER BY id DESC
    """
    )
    suspend fun getMessagesByName(name: String): List<MessageEntity>

    @Query(
        """
        SELECT * FROM messages 
        WHERE ([from] = :name OR [to] = :name) 
          AND id < :lastKnownId
        ORDER BY id DESC
        LIMIT :limit
    """
    )
    suspend fun getMessagesByNameWithLimit(
        name: String,
        lastKnownId: Int,
        limit: Int,
    ): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query(
        """
        DELETE FROM messages 
        WHERE id NOT IN (:messageIds)
    """
    )
    suspend fun deleteOldMessages(messageIds: List<Int>)
}
