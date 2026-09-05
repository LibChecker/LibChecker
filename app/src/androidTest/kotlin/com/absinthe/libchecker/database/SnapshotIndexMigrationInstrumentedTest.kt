package com.absinthe.libchecker.database

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotIndexMigrationInstrumentedTest {
  @Test
  fun indexMigrationPreservesRowsAndSupportsExactPackageAndTimestampLookup() = runBlocking {
    SQLiteDatabase.create(null).use { database ->
      database.execSQL(
        "CREATE TABLE snapshot_table (id INTEGER PRIMARY KEY, timeStamp INTEGER NOT NULL, " +
          "packageName TEXT NOT NULL, nativeLibs TEXT NOT NULL)"
      )
      database.execSQL("INSERT INTO snapshot_table VALUES (1, 12, 'com.a_b', 'payload')")
      database.execSQL("INSERT INTO snapshot_table VALUES (2, 12, 'com.axb', 'other')")
      database.execSQL("INSERT INTO snapshot_table VALUES (3, 123, 'com.a_b', 'history')")

      LCDatabase.MIGRATION_24_25.migrate(migrationConnection(database))

      database.rawQuery("SELECT COUNT(*), SUM(id) FROM snapshot_table", null).use {
        assertTrue(it.moveToFirst())
        assertEquals(3L, it.getLong(0))
        assertEquals(6L, it.getLong(1))
      }
      val query = "SELECT nativeLibs FROM snapshot_table WHERE timeStamp = 12 AND packageName = 'com.a_b'"
      database.rawQuery(query, null).use {
        assertTrue(it.moveToFirst())
        assertEquals("payload", it.getString(0))
        assertEquals(false, it.moveToNext())
      }
      // AndroidSQLiteDriver classifies EXPLAIN as a non-query statement; rawQuery supports it.
      database.rawQuery("EXPLAIN QUERY PLAN $query", null).use {
        assertTrue(it.moveToFirst())
        assertTrue(it.getString(3).contains("index_snapshot_table_timeStamp_packageName"))
        assertTrue(it.getString(3).contains("timeStamp=? AND packageName=?"))
      }
    }
  }

  private fun migrationConnection(database: SQLiteDatabase): SQLiteConnection {
    return Proxy.newProxyInstance(
      SQLiteConnection::class.java.classLoader,
      arrayOf(SQLiteConnection::class.java)
    ) { _, method, args ->
      check(method.name == "prepare") { "Unexpected migration operation: ${method.name}" }
      val sql = args!![0] as String
      Proxy.newProxyInstance(
        SQLiteStatement::class.java.classLoader,
        arrayOf(SQLiteStatement::class.java)
      ) { _, statementMethod, _ ->
        when (statementMethod.name) {
          "step" -> {
            database.execSQL(sql)
            false
          }

          "close" -> null

          else -> error("Unexpected migration statement operation: ${statementMethod.name}")
        }
      }
    } as SQLiteConnection
  }
}
