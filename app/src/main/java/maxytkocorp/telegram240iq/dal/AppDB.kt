package maxytkocorp.telegram240iq.dal

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import maxytkocorp.telegram240iq.R

@Database(entities = [MessageEntity::class, ChatEntity::class], version = 1)
abstract class AppDB : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDB? = null

        fun getInstance(context: Context): AppDB {
            return INSTANCE ?: createInstance(context).also {
                INSTANCE = it
            }
        }

        private fun createInstance(context: Context): AppDB {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDB::class.java,
                context.getString(R.string.db_name)
            ).build()
        }
    }
}
