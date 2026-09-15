package com.absinthe.libchecker.utils

import java.io.File

object FileUtils {

  fun delete(file: File?): Boolean {
    return file?.deleteRecursively() ?: false
  }

  fun getFileSize(file: File): Long {
    return file.length()
  }

  fun getFileSize(path: String): Long {
    return File(path).length()
  }
}
