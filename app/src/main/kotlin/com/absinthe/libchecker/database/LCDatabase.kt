package com.absinthe.libchecker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LCItem::class, NativeLibItem::class], version = 1, exportSchema = false)
public abstract class LCDatabase : RoomDatabase() {

    abstract fun lcDao(): LCDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: LCDatabase? = null

        fun getDatabase(context: Context): LCDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LCDatabase::class.java,
                    "lc_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}