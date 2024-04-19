package com.absinthe.libchecker.database.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.fragment.app.FragmentActivity
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Locale
import javax.crypto.BadPaddingException
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.comparator.LastModifiedFileComparator
import timber.log.Timber

/**
 *  MIT License
 *
 *  Copyright (c) 2022 Raphael Ebner
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
class RoomBackup(var context: Context) : FragmentActivity() {

  companion object {
    private const val SHARED_PREFS = "de.raphaelebner.roomdatabasebackup"
    private var TAG = "debug_RoomBackup"
    private lateinit var INTERNAL_BACKUP_PATH: File
    private lateinit var TEMP_BACKUP_PATH: File
    private lateinit var TEMP_BACKUP_FILE: File
    private lateinit var EXTERNAL_BACKUP_PATH: File
    private lateinit var DATABASE_FILE: File

    private var currentProcess: Int? = null
    private const val PROCESS_BACKUP = 1
    private const val PROCESS_RESTORE = 2
    private var backupFilename: String? = null

    /**
     * Code for internal backup location, used for [backupLocation]
     */
    const val BACKUP_FILE_LOCATION_INTERNAL = 1

    /**
     * Code for external backup location, used for [backupLocation]
     */
    const val BACKUP_FILE_LOCATION_EXTERNAL = 2

    /**
     * Code for custom backup location dialog, used for [backupLocation]
     */
    const val BACKUP_FILE_LOCATION_CUSTOM_DIALOG = 3

    /**
     * Code for custom backup file location, used for [backupLocation]
     */
    const val BACKUP_FILE_LOCATION_CUSTOM_FILE = 4
  }

  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var dbName: String

  private var roomDatabase: RoomDatabase? = null
  private var enableLogDebug: Boolean = false
  private var restartIntent: Intent? = null
  private var onCompleteListener: OnCompleteListener? = null
  private var customRestoreDialogTitle: String = "Choose file to restore"
  private var customBackupFileName: String? = null
  private var backupIsEncrypted: Boolean = false
  private var maxFileCount: Int? = null
  private var encryptPassword: String? = null
  private var backupLocation: Int = BACKUP_FILE_LOCATION_INTERNAL
  private var backupLocationCustomFile: File? = null

  /**
   * Set RoomDatabase instance
   *
   * @param roomDatabase RoomDatabase
   */
  fun database(roomDatabase: RoomDatabase): RoomBackup {
    this.roomDatabase = roomDatabase
    return this
  }

  /**
   * Set LogDebug enabled / disabled
   *
   * @param enableLogDebug Boolean
   */
  fun enableLogDebug(enableLogDebug: Boolean): RoomBackup {
    this.enableLogDebug = enableLogDebug
    return this
  }

  /**
   * Set Intent in which to boot after App restart
   *
   * @param restartIntent Intent
   */
  fun restartApp(restartIntent: Intent): RoomBackup {
    this.restartIntent = restartIntent
    restartApp()
    return this
  }

  /**
   * Set onCompleteListener, to run code when tasks completed
   *
   * @param onCompleteListener OnCompleteListener
   */
  fun onCompleteListener(onCompleteListener: OnCompleteListener): RoomBackup {
    this.onCompleteListener = onCompleteListener
    return this
  }

  /**
   * Set onCompleteListener, to run code when tasks completed
   *
   * @param listener (success: Boolean, message: String) -> Unit
   */
  fun onCompleteListener(listener: (success: Boolean, message: String, exitCode: Int) -> Unit): RoomBackup {
    this.onCompleteListener = object : OnCompleteListener {
      override fun onComplete(success: Boolean, message: String, exitCode: Int) {
        listener(success, message, exitCode)
      }
    }
    return this
  }

  /**
   * Set custom log tag, for detailed debugging
   *
   * @param customLogTag String
   */
  fun customLogTag(customLogTag: String): RoomBackup {
    TAG = customLogTag
    return this
  }

  /**
   * Set custom Restore Dialog Title, default = "Choose file to restore"
   *
   * @param customRestoreDialogTitle String
   */
  fun customRestoreDialogTitle(customRestoreDialogTitle: String): RoomBackup {
    this.customRestoreDialogTitle = customRestoreDialogTitle
    return this
  }

  /**
   * Set custom Backup File Name, default = "$dbName-$currentTime.sqlite3"
   *
   * @param customBackupFileName String
   */
  fun customBackupFileName(customBackupFileName: String): RoomBackup {
    this.customBackupFileName = customBackupFileName
    return this
  }

  /**
   * Set you backup location. Available values see: [BACKUP_FILE_LOCATION_INTERNAL], [BACKUP_FILE_LOCATION_EXTERNAL], [BACKUP_FILE_LOCATION_CUSTOM_DIALOG] or [BACKUP_FILE_LOCATION_CUSTOM_FILE]
   *
   *
   * @param backupLocation Int, default = [BACKUP_FILE_LOCATION_INTERNAL]
   */
  fun backupLocation(backupLocation: Int): RoomBackup {
    this.backupLocation = backupLocation
    return this
  }

  /**
   * Set a custom file where to save or restore a backup.
   * can be used for backup and restore
   *
   * Only available if [backupLocation] is set to [BACKUP_FILE_LOCATION_CUSTOM_FILE]
   *
   * @param backupLocationCustomFile File
   */
  fun backupLocationCustomFile(backupLocationCustomFile: File): RoomBackup {
    this.backupLocationCustomFile = backupLocationCustomFile
    return this
  }

  /**
   * Set file encryption to true / false
   * can be used for backup and restore
   *
   *
   * @param backupIsEncrypted Boolean, default = false
   */
  fun backupIsEncrypted(backupIsEncrypted: Boolean): RoomBackup {
    this.backupIsEncrypted = backupIsEncrypted
    return this
  }

  /**
   * Set max backup files count
   * if fileCount is > maxFileCount the oldest backup file will be deleted
   * is for both internal and external storage
   *
   *
   * @param maxFileCount Int, default = null
   */
  fun maxFileCount(maxFileCount: Int): RoomBackup {
    this.maxFileCount = maxFileCount
    return this
  }

  /**
   * Set custom backup encryption password
   *
   * @param encryptPassword String
   */

  fun customEncryptPassword(encryptPassword: String): RoomBackup {
    this.encryptPassword = encryptPassword
    return this
  }

  /**
   * Init vars, and return true if no error occurred
   */
  private fun initRoomBackup(): Boolean {
    if (roomDatabase == null) {
      if (enableLogDebug) Timber.d("roomDatabase is missing")
      onCompleteListener?.onComplete(
        false,
        "roomDatabase is missing",
        OnCompleteListener.EXIT_CODE_ERROR_ROOM_DATABASE_MISSING
      )
      //        throw IllegalArgumentException("roomDatabase is not initialized")
      return false
    }

    // Create or retrieve the Master Key for encryption/decryption
    val masterKeyAlias = MasterKey.Builder(context)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()

    if (backupLocation !in listOf(
        BACKUP_FILE_LOCATION_INTERNAL,
        BACKUP_FILE_LOCATION_EXTERNAL,
        BACKUP_FILE_LOCATION_CUSTOM_DIALOG,
        BACKUP_FILE_LOCATION_CUSTOM_FILE
      )
    ) {
      if (enableLogDebug) Timber.d("backupLocation is missing")
      onCompleteListener?.onComplete(
        false,
        "backupLocation is missing",
        OnCompleteListener.EXIT_CODE_ERROR_BACKUP_LOCATION_MISSING
      )
      return false
    }

    if (backupLocation == BACKUP_FILE_LOCATION_CUSTOM_FILE && backupLocationCustomFile == null) {
      if (enableLogDebug) {
        Timber.d("backupLocation is set to custom backup file, but no file is defined")
      }
      onCompleteListener?.onComplete(
        false,
        "backupLocation is set to custom backup file, but no file is defined",
        OnCompleteListener.EXIT_CODE_ERROR_BACKUP_LOCATION_FILE_MISSING
      )
      return false
    }

    // Initialize/open an instance of EncryptedSharedPreferences
    // Encryption key is stored in plain text in an EncryptedSharedPreferences --> it is saved encrypted
    sharedPreferences = EncryptedSharedPreferences.create(
      context,
      SHARED_PREFS,
      masterKeyAlias,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    dbName = roomDatabase!!.openHelper.databaseName!!
    INTERNAL_BACKUP_PATH = File("${context.filesDir}/databasebackup/")
    TEMP_BACKUP_PATH = File("${context.filesDir}/databasebackup-temp/")
    TEMP_BACKUP_FILE = File("$TEMP_BACKUP_PATH/tempbackup.sqlite3")
    EXTERNAL_BACKUP_PATH = File(context.getExternalFilesDir("backup")!!.toURI())
    DATABASE_FILE = File(context.getDatabasePath(dbName).toURI())

    // Create internal and temp backup directory if does not exist
    try {
      INTERNAL_BACKUP_PATH.mkdirs()
      TEMP_BACKUP_PATH.mkdirs()
    } catch (_: FileAlreadyExistsException) {
    } catch (_: IOException) {
    }

    if (enableLogDebug) {
      Timber.d("DatabaseName: $dbName")
      Timber.d("Database Location: $DATABASE_FILE")
      Timber.d("INTERNAL_BACKUP_PATH: $INTERNAL_BACKUP_PATH")
      Timber.d("EXTERNAL_BACKUP_PATH: $EXTERNAL_BACKUP_PATH")
      if (backupLocationCustomFile != null) {
        Timber.d("backupLocationCustomFile: $backupLocationCustomFile")
      }
    }
    return true
  }

  /**
   * restart App with custom Intent
   */
  private fun restartApp() {
    restartIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(restartIntent)
    if (context is Activity) {
      (context as Activity).finish()
    }
    Runtime.getRuntime().exit(0)
  }

  /**
   * Start Backup process, and set onComplete Listener to success, if no error occurred, else onComplete Listener success is false
   * and error message is passed
   *
   * if custom storage ist selected, the [openBackupfileCreator] will be launched
   */
  fun backup() {
    if (enableLogDebug) Timber.d("Starting Backup ...")
    val success = initRoomBackup()
    if (!success) return

    // Needed for storage permissions request
    currentProcess = PROCESS_BACKUP

    // Create name for backup file, if no custom name is set: Database name + currentTime + .sqlite3
    var filename =
      if (customBackupFileName == null) "$dbName-${getTime()}.sqlite3" else customBackupFileName as String
    // Add .aes extension to filename, if file is encrypted
    if (backupIsEncrypted) filename += ".aes"
    if (enableLogDebug) Timber.d("backupFilename: $filename")

    when (backupLocation) {
      BACKUP_FILE_LOCATION_INTERNAL -> {
        val backupFile = File("$INTERNAL_BACKUP_PATH/$filename")
        doBackup(backupFile)
      }

      BACKUP_FILE_LOCATION_EXTERNAL -> {
        val backupFile = File("$EXTERNAL_BACKUP_PATH/$filename")
        doBackup(backupFile)
      }

      BACKUP_FILE_LOCATION_CUSTOM_DIALOG -> {
        backupFilename = filename
        openBackupfileCreator.launch(backupFilename!!)
        return
      }

      BACKUP_FILE_LOCATION_CUSTOM_FILE -> {
        doBackup(backupLocationCustomFile!!)
      }

      else -> return
    }
  }

  /**
   * This method will do the backup action
   *
   * @param destination File
   */
  private fun doBackup(destination: File) {
    // Close the database
    roomDatabase!!.close()
    if (backupIsEncrypted) {
      val encryptedBytes = encryptBackup() ?: return
      val bos = BufferedOutputStream(FileOutputStream(destination, false))
      bos.write(encryptedBytes)
      bos.flush()
      bos.close()
    } else {
      // Copy current database to save location (/files dir)
      DATABASE_FILE.copyTo(destination)
    }

    // If maxFileCount is set and is reached, delete oldest file
    if (maxFileCount != null) {
      val deleted = deleteOldBackup()
      if (!deleted) return
    }

    if (enableLogDebug) {
      Timber.d("Backup done, encrypted($backupIsEncrypted) and saved to $destination")
    }
    onCompleteListener?.onComplete(true, "success", OnCompleteListener.EXIT_CODE_SUCCESS)
  }

  /**
   * This method will do the backup action
   *
   * @param destination OutputStream
   */
  private fun doBackup(destination: OutputStream) {
    // Close the database
    roomDatabase!!.close()
    if (backupIsEncrypted) {
      val encryptedBytes = encryptBackup() ?: return
      destination.write(encryptedBytes)
    } else {
      // Copy current database to save location (/files dir)
      DATABASE_FILE.inputStream().use { input ->
        destination.use { output ->
          input.copyTo(output)
        }
      }
    }

    // If maxFileCount is set and is reached, delete oldest file
    if (maxFileCount != null) {
      val deleted = deleteOldBackup()
      if (!deleted) return
    }
    if (enableLogDebug) {
      Timber.d("Backup done, encrypted($backupIsEncrypted) and saved to $destination")
    }
    onCompleteListener?.onComplete(true, "success", OnCompleteListener.EXIT_CODE_SUCCESS)
  }

  /**
   * Encrypts the current Database and return it's content as ByteArray.
   * The original Database is not encrypted only a current copy of this database
   *
   * @return encrypted backup as ByteArray
   */
  private fun encryptBackup(): ByteArray? {
    try {
      // Copy database you want to backup to temp directory
      DATABASE_FILE.copyTo(TEMP_BACKUP_FILE)

      // encrypt temp file, and save it to backup location
      val encryptDecryptBackup = AESEncryptionHelper()
      val fileData = encryptDecryptBackup.readFile(TEMP_BACKUP_FILE)

      val aesEncryptionManager = AESEncryptionManager()
      val encryptedBytes =
        aesEncryptionManager.encryptData(sharedPreferences, encryptPassword, fileData)

      // Delete temp file
      TEMP_BACKUP_FILE.delete()

      return encryptedBytes
    } catch (e: Exception) {
      if (enableLogDebug) Timber.d("error during encryption: ${e.message}")
      onCompleteListener?.onComplete(
        false,
        "error during encryption",
        OnCompleteListener.EXIT_CODE_ERROR_ENCRYPTION_ERROR
      )
      return null
    }
  }

  /**
   * Start Restore process, and set onComplete Listener to success, if no error occurred, else onComplete Listener success is false and error message is passed
   *
   * if internal or external storage is selected, this function shows a list of all available backup files in a MaterialAlertDialog and
   * calls [restoreSelectedInternalExternalFile] to restore selected file
   *
   * if custom storage ist selected, the [openBackupfileChooser] will be launched
   */
  fun restore() {
    if (enableLogDebug) Timber.d("Starting Restore ...")
    val success = initRoomBackup()
    if (!success) return

    // Needed for storage permissions request
    currentProcess = PROCESS_RESTORE

    // Path of Backup Directory
    val backupDirectory: File

    when (backupLocation) {
      BACKUP_FILE_LOCATION_INTERNAL -> {
        backupDirectory = INTERNAL_BACKUP_PATH
      }

      BACKUP_FILE_LOCATION_EXTERNAL -> {
        backupDirectory = File("$EXTERNAL_BACKUP_PATH/")
      }

      BACKUP_FILE_LOCATION_CUSTOM_DIALOG -> {
        openBackupfileChooser.launch(arrayOf("*/*"))
        return
      }

      BACKUP_FILE_LOCATION_CUSTOM_FILE -> {
        Timber.d("backupLocationCustomFile!!.exists()? : ${backupLocationCustomFile!!.exists()}")
        doRestore(backupLocationCustomFile!!)
        return
      }

      else -> return
    }

    // All Files in an Array of type File
    val arrayOfFiles = backupDirectory.listFiles()

    // If array is null or empty show "error" and return
    if (arrayOfFiles.isNullOrEmpty()) {
      if (enableLogDebug) Timber.d("No backups available to restore")
      onCompleteListener?.onComplete(
        false,
        "No backups available",
        OnCompleteListener.EXIT_CODE_ERROR_RESTORE_NO_BACKUPS_AVAILABLE
      )
      Toast.makeText(context, "No backups available to restore", Toast.LENGTH_SHORT).show()
      return
    }

    // Sort Array: lastModified
    Arrays.sort(arrayOfFiles, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR)

    // New empty MutableList of String
    val mutableListOfFilesAsString = mutableListOf<String>()

    // Add each filename to mutablelistOfFilesAsString
    runBlocking {
      for (i in arrayOfFiles.indices) {
        mutableListOfFilesAsString.add(arrayOfFiles[i].name)
      }
    }

    // Convert MutableList to Array
    val filesStringArray = mutableListOfFilesAsString.toTypedArray()

    // Show MaterialAlertDialog, with all available files, and on click Listener
    MaterialAlertDialogBuilder(context)
      .setTitle(customRestoreDialogTitle)
      .setItems(filesStringArray) { _, which ->
        restoreSelectedInternalExternalFile(filesStringArray[which])
      }
      .setOnCancelListener {
        if (enableLogDebug) Timber.d("Restore dialog canceled")
        onCompleteListener?.onComplete(
          false,
          "Restore dialog canceled",
          OnCompleteListener.EXIT_CODE_ERROR_BY_USER_CANCELED
        )
      }
      .show()
  }

  /**
   * This method will do the restore action
   *
   * @param source File
   */
  private fun doRestore(source: File) {
    // Close the database
    roomDatabase!!.close()
    val fileExtension = source.extension
    if (backupIsEncrypted) {
      source.copyTo(TEMP_BACKUP_FILE)
      val decryptedBytes = decryptBackup() ?: return
      val bos = BufferedOutputStream(FileOutputStream(DATABASE_FILE, false))
      bos.write(decryptedBytes)
      bos.flush()
      bos.close()
    } else {
      if (fileExtension == "aes") {
        if (enableLogDebug) {
          Timber.d("Cannot restore database, it is encrypted. Maybe you forgot to add the property .fileIsEncrypted(true)")
        }
        onCompleteListener?.onComplete(
          false,
          "cannot restore database, see Log for more details (if enabled)",
          OnCompleteListener.EXIT_CODE_ERROR_RESTORE_BACKUP_IS_ENCRYPTED
        )
        return
      }
      // Copy back database and replace current database
      source.copyTo(DATABASE_FILE)
    }

    if (enableLogDebug) {
      Timber.d("Restore done, decrypted($backupIsEncrypted) and restored from $source")
    }
    onCompleteListener?.onComplete(true, "success", OnCompleteListener.EXIT_CODE_SUCCESS)
  }

  /**
   * This method will do the restore action
   *
   * @param source InputStream
   */
  private fun doRestore(source: InputStream) {
    if (backupIsEncrypted) {
      // Save inputstream to temp file
      source.use { input ->
        TEMP_BACKUP_FILE.outputStream().use { output ->
          input.copyTo(output)
        }
      }
      // Decrypt tempfile and write to database file
      val decryptedBytes = decryptBackup() ?: return

      // Close the database if decryption is succesfull
      roomDatabase!!.close()

      val bos = BufferedOutputStream(FileOutputStream(DATABASE_FILE, false))
      bos.write(decryptedBytes)
      bos.flush()
      bos.close()
    } else {
      // Close the database
      roomDatabase!!.close()

      // Copy back database and replace current database
      source.use { input ->
        DATABASE_FILE.outputStream().use { output ->
          input.copyTo(output)
        }
      }
    }

    if (enableLogDebug) {
      Timber.d("Restore done, decrypted($backupIsEncrypted) and restored from $source")
    }
    onCompleteListener?.onComplete(true, "success", OnCompleteListener.EXIT_CODE_SUCCESS)
  }

  /**
   * Restores the selected file from internal or external storage
   *
   * @param filename String
   */
  private fun restoreSelectedInternalExternalFile(filename: String) {
    if (enableLogDebug) Timber.d("Restore selected file...")

    when (backupLocation) {
      BACKUP_FILE_LOCATION_INTERNAL -> {
        doRestore(File("$INTERNAL_BACKUP_PATH/$filename"))
      }

      BACKUP_FILE_LOCATION_EXTERNAL -> {
        doRestore(File("$EXTERNAL_BACKUP_PATH/$filename"))
      }

      else -> return
    }
  }

  /**
   * Decrypts the [TEMP_BACKUP_FILE] and return it's content as ByteArray.
   * A valid encrypted backup file must be present on [TEMP_BACKUP_FILE]
   *
   * @return decrypted backup as ByteArray
   */
  private fun decryptBackup(): ByteArray? {
    try {
      // Decrypt temp file, and save it to database location
      val encryptDecryptBackup = AESEncryptionHelper()
      val fileData = encryptDecryptBackup.readFile(TEMP_BACKUP_FILE)

      val aesEncryptionManager = AESEncryptionManager()
      val decryptedBytes =
        aesEncryptionManager.decryptData(sharedPreferences, encryptPassword, fileData)

      // Delete tem file
      TEMP_BACKUP_FILE.delete()

      return decryptedBytes
    } catch (e: BadPaddingException) {
      if (enableLogDebug) Timber.d("error during decryption (wrong password): ${e.message}")
      onCompleteListener?.onComplete(
        false,
        "error during decryption (wrong password) see Log for more details (if enabled)",
        OnCompleteListener.EXIT_CODE_ERROR_WRONG_DECRYPTION_PASSWORD
      )
      return null
    } catch (e: Exception) {
      if (enableLogDebug) Timber.d("error during decryption: ${e.message}")
      onCompleteListener?.onComplete(
        false,
        "error during decryption see Log for more details (if enabled)",
        OnCompleteListener.EXIT_CODE_ERROR_DECRYPTION_ERROR
      )
      return null
    }
  }

  /**
   * Opens the [ActivityResultContracts.RequestMultiplePermissions] and prompts the user to grant storage permissions
   *
   * If granted backup or restore process starts
   */
  private val permissionRequestLauncher =
    (context as ComponentActivity).registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
      permissions.entries.forEach {
        if (!it.value) {
          onCompleteListener?.onComplete(
            false,
            "storage permissions are required, please allow!",
            OnCompleteListener.EXIT_CODE_ERROR_STORAGE_PERMISSONS_NOT_GRANTED
          )
          return@registerForActivityResult
        }
      }
      when (currentProcess) {
        PROCESS_BACKUP -> {
          backupFilename?.let { openBackupfileCreator.launch(it) }
        }

        PROCESS_RESTORE -> {
          openBackupfileChooser.launch(arrayOf("*/*"))
        }
      }
    }

  /**
   * Opens the [ActivityResultContracts.OpenDocument] and prompts the user to open a document for restoring a backup file
   */
  private val openBackupfileChooser =
    (context as ComponentActivity).registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
      if (result != null) {
        val inputstream = context.contentResolver.openInputStream(result)!!
        doRestore(inputstream)
        return@registerForActivityResult
      }
      onCompleteListener?.onComplete(
        false,
        "failure",
        OnCompleteListener.EXIT_CODE_ERROR_BACKUP_FILE_CHOOSER
      )
    }

  /**
   * Opens the [ActivityResultContracts.CreateDocument] and prompts the user to select a path for creating the new backup file
   */
  private val openBackupfileCreator =
    (context as ComponentActivity).registerForActivityResult(CreateDocument("todo/todo")) { result ->
      if (result != null) {
        val out = context.contentResolver.openOutputStream(result)!!
        doBackup(out)
        return@registerForActivityResult
      }
      onCompleteListener?.onComplete(
        false,
        "failure",
        OnCompleteListener.EXIT_CODE_ERROR_BACKUP_FILE_CREATOR
      )
    }

  /**
   * @return current time formatted as String
   */
  private fun getTime(): String {
    val currentTime = Calendar.getInstance().time

    val sdf = if (Build.VERSION.SDK_INT <= 28) {
      SimpleDateFormat("yyyy-MM-dd-HH_mm_ss", Locale.getDefault())
    } else {
      SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.getDefault())
    }

    return sdf.format(currentTime)
  }

  /**
   * If maxFileCount is set, and reached, all old files will be deleted. Only if [BACKUP_FILE_LOCATION_INTERNAL] or [BACKUP_FILE_LOCATION_EXTERNAL]
   *
   * @return true if old files deleted or nothing to do
   */
  private fun deleteOldBackup(): Boolean {
    // Path of Backup Directory

    val backupDirectory: File = when (backupLocation) {
      BACKUP_FILE_LOCATION_INTERNAL -> {
        INTERNAL_BACKUP_PATH
      }

      BACKUP_FILE_LOCATION_EXTERNAL -> {
        File("$EXTERNAL_BACKUP_PATH/")
      }

      BACKUP_FILE_LOCATION_CUSTOM_DIALOG -> {
        // In custom backup location no backups will be removed
        return true
      }

      else -> return true
    }

    // All Files in an Array of type File
    val arrayOfFiles = backupDirectory.listFiles()

    // If array is null or empty nothing to do and return
    if (arrayOfFiles.isNullOrEmpty()) {
      if (enableLogDebug) Timber.d("")
      onCompleteListener?.onComplete(
        false,
        "maxFileCount: Failed to get list of backups",
        OnCompleteListener.EXIT_CODE_ERROR
      )
      return false
    } else if (arrayOfFiles.size > maxFileCount!!) {
      // Sort Array: lastModified
      Arrays.sort(arrayOfFiles, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR)

      // Get count of files to delete
      val fileCountToDelete = arrayOfFiles.size - maxFileCount!!

      for (i in 1..fileCountToDelete) {
        // Delete all old files (i-1 because array starts a 0)
        arrayOfFiles[i - 1].delete()

        if (enableLogDebug) Timber.d("maxFileCount reached: ${arrayOfFiles[i - 1]} deleted")
      }
    }
    return true
  }
}
