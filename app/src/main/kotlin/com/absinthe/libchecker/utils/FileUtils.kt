package com.absinthe.libchecker.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object FileUtils {

  fun delete(file: File?): Boolean {
    return file?.deleteRecursively() ?: false
  }

  fun getFileSize(file: File): Long {
    return getFileSize(file.path)
  }

  fun getFileSize(path: String): Long {
    return if (OsUtils.atLeastO()) {
      runCatching {
        Files.size(Paths.get(path))
      }.getOrDefault(0L)
    } else {
      val file = File(path)
      if (file.exists()) file.length() else 0
    }
  }
}
