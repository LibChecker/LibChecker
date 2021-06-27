package com.absinthe.libchecker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.RuleEntity
import java.io.File

@Database(entities = [RuleEntity::class], version = 1, exportSchema = true)
abstract class RuleDatabase : RoomDatabase() {

    abstract fun ruleDao(): RuleDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: RuleDatabase? = null

        fun getDatabase(context: Context): RuleDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val ruleFile = File(context.cacheDir, Constants.RULES_DB_FILE_NAME)
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    RuleDatabase::class.java,
                    Constants.RULES_DATABASE_NAME
                ).fallbackToDestructiveMigration()
                if (ruleFile.exists()) {
                    builder.createFromFile(ruleFile)
                } else {
                    builder.createFromAsset("database/rules.db")
                }
                INSTANCE = builder.build()
                return INSTANCE!!
            }
        }
    }
}