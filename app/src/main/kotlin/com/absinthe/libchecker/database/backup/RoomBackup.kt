package com.absinthe.libchecker.database.backup

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.lifecycle.lifecycleScope
import androidx.room3.RoomDatabase
import androidx.room3.useWriterConnection
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class RoomBackup(private val context: Context) {

  companion object {
    const val BACKUP_FILE_LOCATION_INTERNAL = 1
    const val BACKUP_FILE_LOCATION_EXTERNAL = 2
    const val BACKUP_FILE_LOCATION_CUSTOM_DIALOG = 3
    const val BACKUP_FILE_LOCATION_CUSTOM_FILE = 4

    private const val SQLITE_DOCUMENT_MIME_TYPE = "application/vnd.sqlite3"
  }

  private var roomDatabase: RoomDatabase? = null
  private var databaseName: String? = null
  private var enableLogDebug: Boolean = false
  private var onCompleteListener: OnCompleteListener? = null
  private var customBackupFileName: String? = null
  private var maxFileCount: Int? = null
  private var backupLocation: Int = BACKUP_FILE_LOCATION_INTERNAL
  private var backupLocationCustomFile: File? = null
  private val backupFileCreator: ActivityResultLauncher<String>? =
    (context as? ComponentActivity)
      ?.registerForActivityResult(CreateDocument(SQLITE_DOCUMENT_MIME_TYPE)) { uri ->
        if (uri == null) {
          complete(
            success = false,
            message = "failure",
            exitCode = OnCompleteListener.EXIT_CODE_ERROR_BACKUP_FILE_CREATOR
          )
          return@registerForActivityResult
        }
        runBackup {
          doBackup(uri)
        }
      }

  fun database(roomDatabase: RoomDatabase, databaseName: String): RoomBackup {
    this.roomDatabase = roomDatabase
    this.databaseName = databaseName
    return this
  }

  fun database(roomDatabase: RoomDatabase): RoomBackup {
    this.roomDatabase = roomDatabase
    return this
  }

  fun enableLogDebug(enableLogDebug: Boolean): RoomBackup {
    this.enableLogDebug = enableLogDebug
    return this
  }

  fun onCompleteListener(onCompleteListener: OnCompleteListener): RoomBackup {
    this.onCompleteListener = onCompleteListener
    return this
  }

  fun onCompleteListener(listener: (success: Boolean, message: String, exitCode: Int) -> Unit): RoomBackup {
    this.onCompleteListener = object : OnCompleteListener {
      override fun onComplete(success: Boolean, message: String, exitCode: Int) {
        listener(success, message, exitCode)
      }
    }
    return this
  }

  fun customBackupFileName(customBackupFileName: String): RoomBackup {
    this.customBackupFileName = customBackupFileName
    return this
  }

  fun backupLocation(backupLocation: Int): RoomBackup {
    this.backupLocation = backupLocation
    return this
  }

  fun backupLocationCustomFile(backupLocationCustomFile: File): RoomBackup {
    this.backupLocationCustomFile = backupLocationCustomFile
    return this
  }

  fun maxFileCount(maxFileCount: Int): RoomBackup {
    this.maxFileCount = maxFileCount
    return this
  }

  fun backup() {
    if (enableLogDebug) {
      Timber.d("Starting database backup")
    }
    if (validateBackupConfiguration().not()) {
      return
    }

    when (backupLocation) {
      BACKUP_FILE_LOCATION_CUSTOM_DIALOG -> {
        val launcher = backupFileCreator ?: run {
          complete(
            success = false,
            message = "Document API not available",
            exitCode = OnCompleteListener.EXIT_CODE_ERROR_BACKUP_FILE_CREATOR
          )
          return
        }
        launcher.launch(resolveBackupFileName())
      }

      BACKUP_FILE_LOCATION_CUSTOM_FILE -> {
        val destination = requireNotNull(backupLocationCustomFile)
        runBackup {
          doBackup(destination)
        }
      }

      BACKUP_FILE_LOCATION_INTERNAL,
      BACKUP_FILE_LOCATION_EXTERNAL -> {
        val destination = File(resolveBackupDirectory(), resolveBackupFileName())
        runBackup {
          doBackup(destination)
        }
      }
    }
  }

  fun restore() {
    if (enableLogDebug) {
      Timber.d("Starting database restore")
    }
    if (validateRestoreConfiguration().not()) {
      return
    }

    when (backupLocation) {
      BACKUP_FILE_LOCATION_CUSTOM_FILE -> doRestore(requireNotNull(backupLocationCustomFile))

      else -> complete(
        success = false,
        message = "Unsupported restore location",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR_BACKUP_LOCATION_MISSING
      )
    }
  }

  private fun validateBackupConfiguration(): Boolean {
    if (validateCommonConfiguration().not()) {
      return false
    }
    if (backupLocation !in supportedBackupLocations()) {
      complete(
        success = false,
        message = "backupLocation is missing",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR_BACKUP_LOCATION_MISSING
      )
      return false
    }
    if (backupLocation == BACKUP_FILE_LOCATION_CUSTOM_FILE && backupLocationCustomFile == null) {
      complete(
        success = false,
        message = "backupLocation is set to custom backup file, but no file is defined",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR_BACKUP_LOCATION_FILE_MISSING
      )
      return false
    }
    return true
  }

  private fun validateRestoreConfiguration(): Boolean {
    if (validateCommonConfiguration().not()) {
      return false
    }
    if (backupLocation != BACKUP_FILE_LOCATION_CUSTOM_FILE || backupLocationCustomFile == null) {
      complete(
        success = false,
        message = "backupLocation is set to custom backup file, but no file is defined",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR_BACKUP_LOCATION_FILE_MISSING
      )
      return false
    }
    return true
  }

  private fun validateCommonConfiguration(): Boolean {
    if (roomDatabase == null) {
      complete(
        success = false,
        message = "roomDatabase is missing",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR_ROOM_DATABASE_MISSING
      )
      return false
    }
    if (databaseName == null) {
      complete(
        success = false,
        message = "databaseName is missing",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR_ROOM_DATABASE_MISSING
      )
      return false
    }
    return true
  }

  private fun supportedBackupLocations(): Set<Int> {
    return setOf(
      BACKUP_FILE_LOCATION_INTERNAL,
      BACKUP_FILE_LOCATION_EXTERNAL,
      BACKUP_FILE_LOCATION_CUSTOM_DIALOG,
      BACKUP_FILE_LOCATION_CUSTOM_FILE
    )
  }

  private fun runBackup(block: suspend () -> Unit) {
    val activity = context as? ComponentActivity ?: run {
      complete(
        success = false,
        message = "Context is not a ComponentActivity",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR
      )
      return
    }
    activity.lifecycleScope.launch(Dispatchers.IO) {
      block()
    }
  }

  private suspend fun doBackup(destination: File) {
    try {
      destination.parentFile?.mkdirs()
      checkpointDatabase()
      databaseFile().copyTo(destination, overwrite = true)
      deleteOldBackupsIfNeeded(destination.parentFile)
      if (enableLogDebug) {
        Timber.d("Database backup saved to $destination")
      }
      complete(
        success = true,
        message = "success",
        exitCode = OnCompleteListener.EXIT_CODE_SUCCESS
      )
    } catch (e: Exception) {
      Timber.e(e, "Database backup failed")
      complete(
        success = false,
        message = e.message ?: "Database backup failed",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR
      )
    }
  }

  private suspend fun doBackup(destination: Uri) {
    try {
      checkpointDatabase()
      requireNotNull(context.contentResolver.openOutputStream(destination)) {
        "Unable to open backup destination"
      }.use { output ->
        copyDatabaseTo(output)
      }
      if (enableLogDebug) {
        Timber.d("Database backup saved to $destination")
      }
      complete(
        success = true,
        message = "success",
        exitCode = OnCompleteListener.EXIT_CODE_SUCCESS
      )
    } catch (e: Exception) {
      Timber.e(e, "Database backup failed")
      complete(
        success = false,
        message = e.message ?: "Database backup failed",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR
      )
    }
  }

  private fun doRestore(source: File) {
    try {
      if (source.exists().not()) {
        complete(
          success = false,
          message = "Backup file does not exist",
          exitCode = OnCompleteListener.EXIT_CODE_ERROR_BACKUP_FILE_CHOOSER
        )
        return
      }
      if (source.extension.equals("aes", ignoreCase = true)) {
        complete(
          success = false,
          message = "Encrypted RoomBackup files are no longer supported",
          exitCode = OnCompleteListener.EXIT_CODE_ERROR_RESTORE_BACKUP_IS_ENCRYPTED
        )
        return
      }
      if (source.hasSqliteHeader().not()) {
        complete(
          success = false,
          message = "Backup file is not a plain SQLite database",
          exitCode = OnCompleteListener.EXIT_CODE_ERROR_RESTORE_BACKUP_IS_ENCRYPTED
        )
        return
      }

      roomDatabase?.close()
      val databaseFile = databaseFile()
      databaseFile.parentFile?.mkdirs()
      deleteDatabaseSidecars(databaseFile)
      source.copyTo(databaseFile, overwrite = true)
      deleteDatabaseSidecars(databaseFile)

      if (enableLogDebug) {
        Timber.d("Database restored from $source")
      }
      complete(
        success = true,
        message = "success",
        exitCode = OnCompleteListener.EXIT_CODE_SUCCESS
      )
    } catch (e: Exception) {
      Timber.e(e, "Database restore failed")
      complete(
        success = false,
        message = e.message ?: "Database restore failed",
        exitCode = OnCompleteListener.EXIT_CODE_ERROR
      )
    }
  }

  private suspend fun checkpointDatabase() {
    roomDatabase?.useWriterConnection { connection ->
      connection.usePrepared("PRAGMA wal_checkpoint(TRUNCATE)") { statement ->
        if (statement.step()) {
          val busy = statement.getLong(0)
          if (busy != 0L) {
            throw IOException("Database checkpoint is busy")
          }
        }
      }
    }
  }

  private fun copyDatabaseTo(output: OutputStream) {
    databaseFile().inputStream().use { input ->
      input.copyTo(output)
    }
  }

  private fun databaseFile(): File {
    return context.getDatabasePath(requireNotNull(databaseName))
  }

  private fun resolveBackupFileName(): String {
    return customBackupFileName ?: "${requireNotNull(databaseName)}.sqlite3"
  }

  private fun resolveBackupDirectory(): File {
    return when (backupLocation) {
      BACKUP_FILE_LOCATION_INTERNAL -> File(context.filesDir, "databasebackup")
      BACKUP_FILE_LOCATION_EXTERNAL -> requireNotNull(context.getExternalFilesDir("backup"))
      else -> error("Unsupported backup directory")
    }
  }

  private fun deleteOldBackupsIfNeeded(backupDirectory: File?) {
    val maxCount = maxFileCount ?: return
    val files = backupDirectory?.listFiles()?.sortedBy { it.lastModified() } ?: return
    val removeCount = files.size - maxCount
    if (removeCount <= 0) {
      return
    }
    files.take(removeCount).forEach { file ->
      if (file.delete() && enableLogDebug) {
        Timber.d("Deleted old backup: $file")
      }
    }
  }

  private fun deleteDatabaseSidecars(databaseFile: File) {
    listOf("-wal", "-shm", "-journal")
      .map { suffix -> File("${databaseFile.absolutePath}$suffix") }
      .forEach { sidecar ->
        if (sidecar.exists() && sidecar.delete().not()) {
          Timber.w("Failed to delete database sidecar: $sidecar")
        }
      }
  }

  private fun File.hasSqliteHeader(): Boolean {
    val expectedHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    if (length() < expectedHeader.size) {
      return false
    }
    val actualHeader = ByteArray(expectedHeader.size)
    inputStream().use { input ->
      return input.read(actualHeader) == expectedHeader.size &&
        actualHeader.contentEquals(expectedHeader)
    }
  }

  private fun complete(success: Boolean, message: String, exitCode: Int) {
    onCompleteListener?.onComplete(success, message, exitCode)
  }
}
