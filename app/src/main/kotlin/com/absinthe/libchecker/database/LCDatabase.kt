package com.absinthe.libchecker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.database.entity.SnapshotItem

@Database(entities = [LCItem::class, SnapshotItem::class], version = 5, exportSchema = false)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE snapshot_table ADD COLUMN timeStamp INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE snapshot_table ADD COLUMN permissions TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE timestamp_table (timestamp INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(timestamp))"
                )
            }
        }
    }
}