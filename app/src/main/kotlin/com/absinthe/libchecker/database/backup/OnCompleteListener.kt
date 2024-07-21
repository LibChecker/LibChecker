package com.absinthe.libchecker.database.backup

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
interface OnCompleteListener {
  fun onComplete(success: Boolean, message: String, exitCode: Int)

  companion object {

    /** Other Error */
    const val EXIT_CODE_ERROR = 1

    /** Error while choosing backup to restore. Maybe no file selected */
    const val EXIT_CODE_ERROR_BACKUP_FILE_CHOOSER = 2

    /** Error while choosing backup file to create. Maybe no file selected */
    const val EXIT_CODE_ERROR_BACKUP_FILE_CREATOR = 3

    /** [BACKUP_FILE_LOCATION_CUSTOM_FILE] is set but [RoomBackup.backupLocationCustomFile] is not set */
    const val EXIT_CODE_ERROR_BACKUP_LOCATION_FILE_MISSING = 4

    /** [RoomBackup.backupLocation] is not set */
    const val EXIT_CODE_ERROR_BACKUP_LOCATION_MISSING = 5

    /** Restore dialog for internal/external storage was canceled by user */
    const val EXIT_CODE_ERROR_BY_USER_CANCELED = 6

    /** Cannot decrypt provided backup file */
    const val EXIT_CODE_ERROR_DECRYPTION_ERROR = 7

    /** Cannot encrypt database backup */
    const val EXIT_CODE_ERROR_ENCRYPTION_ERROR = 8

    /** You tried to restore a encrypted backup but [RoomBackup.backupIsEncrypted] is set to false */
    const val EXIT_CODE_ERROR_RESTORE_BACKUP_IS_ENCRYPTED = 9

    /** No backups to restore are available in internal/external sotrage */
    const val EXIT_CODE_ERROR_RESTORE_NO_BACKUPS_AVAILABLE = 10

    /** No room database to backup is provided  */
    const val EXIT_CODE_ERROR_ROOM_DATABASE_MISSING = 11

    /** Storage permissions not granted for custom dialog */
    const val EXIT_CODE_ERROR_STORAGE_PERMISSONS_NOT_GRANTED = 12

    /** Cannot decrypt provided backup file because the password is incorrect */
    const val EXIT_CODE_ERROR_WRONG_DECRYPTION_PASSWORD = 13

    /** No error, action successful */
    const val EXIT_CODE_SUCCESS = 0
  }
}
