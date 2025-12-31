package ru.faustyu.paprika.data.db

import android.content.Context
import androidx.room.Room

object DatabaseModule {
    private var database: AppDatabase? = null

    fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "paprika_db"
            )
            .fallbackToDestructiveMigration()
            .build()
            database = instance
            instance
        }
    }
}
