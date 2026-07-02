package com.absinthe.libchecker.database.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.database.LCDatabase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.BuildSnapshotRestorePlanUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.SnapshotRestorePlan
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomBackupInstrumentedTest {

  private val context: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

  @Before
  fun setUp() {
    cleanTestFiles()
  }

  @After
  fun tearDown() {
    cleanTestFiles()
  }

  @Test
  fun restoresPlainSqliteBackupCreatedByLegacyRoomBackup() = runBlocking {
    val legacyBackup = createLegacyPlainSqliteBackup()
    var restoreResult: RestoreResult? = null

    RoomBackup(context)
      .database(LCDatabase.getDatabase(), TEST_DATABASE_NAME)
      .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
      .backupLocationCustomFile(legacyBackup)
      .onCompleteListener { success, message, exitCode ->
        restoreResult = RestoreResult(success, message, exitCode)
      }
      .restore()

    assertNotNull(restoreResult)
    assertTrue(restoreResult?.message.orEmpty(), restoreResult?.success == true)
    assertEquals(OnCompleteListener.EXIT_CODE_SUCCESS, restoreResult?.exitCode)
    assertRestoredDatabaseContent()
  }

  @Test
  fun detectsPlainSqliteBackupByHeaderWithoutSqliteExtension() = runBlocking {
    val legacyBackup = createLegacyPlainSqliteBackup()
    val restorePlan = BuildSnapshotRestorePlanUseCase(context.contentResolver)(Uri.fromFile(legacyBackup))

    assertEquals(SnapshotRestorePlan.DatabaseBackup, restorePlan)
  }

  private fun createLegacyPlainSqliteBackup(): File {
    val legacyBackup = File(context.cacheDir, LEGACY_BACKUP_FILE_NAME)
    SQLiteDatabase.openOrCreateDatabase(legacyBackup, null).use { database ->
      database.execSQL(
        "CREATE TABLE legacy_roombackup (" +
          "id INTEGER NOT NULL PRIMARY KEY, " +
          "value TEXT NOT NULL)"
      )
      database.execSQL(
        "INSERT INTO legacy_roombackup (id, value) VALUES (1, 'restored')"
      )
    }
    return legacyBackup
  }

  private fun assertRestoredDatabaseContent() {
    val restoredDatabase = context.getDatabasePath(TEST_DATABASE_NAME)
    assertTrue(restoredDatabase.exists())

    SQLiteDatabase.openDatabase(
      restoredDatabase.absolutePath,
      null,
      SQLiteDatabase.OPEN_READONLY
    ).use { database ->
      database.rawQuery(
        "SELECT value FROM legacy_roombackup WHERE id = 1",
        emptyArray()
      ).use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("restored", cursor.getString(0))
      }
    }
  }

  private fun cleanTestFiles() {
    context.deleteDatabase(TEST_DATABASE_NAME)
    File(context.cacheDir, LEGACY_BACKUP_FILE_NAME).delete()
    context.getDatabasePath(TEST_DATABASE_NAME).deleteSidecars()
  }

  private fun File.deleteSidecars() {
    listOf("-wal", "-shm", "-journal")
      .map { suffix -> File("$absolutePath$suffix") }
      .forEach { it.delete() }
  }

  private data class RestoreResult(
    val success: Boolean,
    val message: String,
    val exitCode: Int
  )

  private companion object {
    private const val TEST_DATABASE_NAME = "roombackup_compat_test"
    private const val LEGACY_BACKUP_FILE_NAME = "legacy-roombackup"
  }
}
