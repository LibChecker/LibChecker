package com.absinthe.libchecker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.absinthe.libchecker.database.entity.*

@Database(entities = [LCItem::class,
    SnapshotItem::class, TimeStampItem::class,
    TrackItem::class, SnapshotDiffStoringItem::class],
    version = 16, exportSchema = true)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                        MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                        MIGRATION_15_16
                    )
                    .createFromAsset("database/rules.db")
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

        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //Add TimeStampItem entity
            }
        }

        private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                    "CREATE TABLE snapshot_new (id INTEGER, packageName TEXT NOT NULL, timeStamp INTEGER NOT NULL DEFAULT 0, label TEXT NOT NULL, versionName TEXT NOT NULL, versionCode INTEGER NOT NULL, installedTime INTEGER NOT NULL, lastUpdatedTime INTEGER NOT NULL, isSystem INTEGER NOT NULL, abi INTEGER NOT NULL, targetApi INTEGER NOT NULL, nativeLibs TEXT NOT NULL, services TEXT NOT NULL, activities TEXT NOT NULL, receivers TEXT NOT NULL, providers TEXT NOT NULL, permissions TEXT NOT NULL, PRIMARY KEY(id))")
                // Copy the data
                database.execSQL(
                    "INSERT INTO snapshot_new (packageName, timeStamp, label, versionName, versionCode, installedTime, lastUpdatedTime, isSystem, abi, targetApi, nativeLibs, services, activities, receivers, providers, permissions) SELECT packageName, timeStamp, label, versionName, versionCode, installedTime, lastUpdatedTime, isSystem, abi, targetApi, nativeLibs, services, activities, receivers, providers, permissions FROM snapshot_table")
                // Remove the old table
                database.execSQL("DROP TABLE snapshot_table")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE snapshot_new RENAME TO snapshot_table")
            }
        }

        private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE track_table (packageName TEXT NOT NULL, PRIMARY KEY(packageName))"
                )
            }
        }

        private val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE rules_table (name TEXT NOT NULL, label TEXT NOT NULL, type INTEGER NOT NULL, iconIndex INTEGER NOT NULL, isRegexRule INTEGER NOT NULL, PRIMARY KEY(name))"
                )
            }
        }

        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE rules_table ADD COLUMN regexName TEXT"
                )
            }
        }

        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE item_table ADD COLUMN targetApi INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE diff_table (packageName TEXT NOT NULL, lastUpdatedTime INTEGER NOT NULL, diffContent TEXT NOT NULL, PRIMARY KEY(packageName))"
                )
            }
        }

        private val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                    "CREATE TABLE rules_new (_id INTEGER NOT NULL DEFAULT 0, name TEXT NOT NULL, label TEXT NOT NULL, type INTEGER NOT NULL DEFAULT 0, iconIndex INTEGER NOT NULL DEFAULT 0, isRegexRule INTEGER NOT NULL DEFAULT 0, regexName TEXT, PRIMARY KEY(_id))")
                // Remove the old table
                database.execSQL("DROP TABLE rules_table")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE rules_new RENAME TO rules_table")
            }
        }

        private val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove the old table
                database.execSQL("DROP TABLE rules_table")
            }
        }

        private val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE timestamp_table ADD COLUMN topApps TEXT")
            }
        }

        private val MIGRATION_15_16: Migration = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE item_table ADD COLUMN variant INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
