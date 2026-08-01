package com.snoretracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SnoreSession::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SnoreDatabase : RoomDatabase() {
    abstract fun snoreDao(): SnoreDao

    companion object {
        @Volatile
        private var INSTANCE: SnoreDatabase? = null

        fun getDatabase(context: Context): SnoreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SnoreDatabase::class.java,
                    "snore_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
