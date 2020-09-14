package com.absinthe.libchecker.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.absinthe.libchecker.constant.Constants

object StorageUtils {
    /* Checks if external storage is available for read and write */
    val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    /* Checks if external storage is available to at least read */
    val isExternalStorageReadable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

    /**
     * Create a file
     *
     * @param activity context
     * @param mimeType MIME type of the file
     * @param fileName file name
     */
    fun createFile(activity: AppCompatActivity, mimeType: String, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            // Create a file with the requested MIME type.
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        try {
            activity.startActivityForResult(intent, Constants.REQUEST_CODE_BACKUP)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Toasty.show(activity, "No Document app")
        }
    }
}