package com.absinthe.libchecker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LCItem::class, SnapshotItem::class], version = 3, exportSchema = false)
abstract class LCDatabase : RoomDatabase() {

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
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS native_lib_table")
                database.execSQL(
                    "CREATE TABLE snapshot_table (packageName TEXT NOT NULL, label TEXT NOT NULL, versionName TEXT NOT NULL, versionCode INTEGER NOT NULL, installedTime INTEGER NOT NULL, lastUpdatedTime INTEGER NOT NULL, isSystem INTEGER NOT NULL, abi INTEGER NOT NULL, targetApi INTEGER NOT NULL, nativeLibs TEXT NOT NULL, services TEXT NOT NULL, activities TEXT NOT NULL, receivers TEXT NOT NULL, providers TEXT NOT NULL, PRIMARY KEY(packageName))"
                )
            }
        }

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE item_table ADD COLUMN isSplitApk INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE item_table ADD COLUMN isKotlinUsed INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}