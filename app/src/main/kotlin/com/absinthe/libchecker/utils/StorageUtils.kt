package com.absinthe.libchecker.utils

import android.os.Environment

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
}
