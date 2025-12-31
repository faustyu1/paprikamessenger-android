package ru.faustyu.paprika.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class, UserEntity::class, SearchHistoryEntity::class, StoryEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun storyDao(): StoryDao
}
