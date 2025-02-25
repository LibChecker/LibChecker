package com.absinthe.libchecker.utils

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream

object FileUtils {

  fun delete(file: File?): Boolean {
    if (file == null) return false
    return if (file.isDirectory) {
      deleteDir(file)
    } else {
      deleteFile(file)
    }
  }

  private fun deleteDir(dir: File?): Boolean {
    if (dir == null) return false
    // dir doesn't exist then return true
    if (!dir.exists()) return true
    // dir isn't a directory then return false
    if (!dir.isDirectory) return false
    val files = dir.listFiles()
    if (files != null && files.isNotEmpty()) {
      for (file in files) {
        if (file.isFile) {
          if (!file.delete()) return false
        } else if (file.isDirectory) {
          if (!deleteDir(file)) return false
        }
      }
    }
    return dir.delete()
  }

  private fun deleteFile(file: File?): Boolean {
    return file != null && (!file.exists() || file.isFile && file.delete())
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

  @Throws(IOException::class)
  fun getEntrySize(zipInput: ZipArchiveInputStream): Int {
    val buffer = ByteArray(1024)
    var totalSize = 0
    var length: Int

    while (zipInput.read(buffer).also { length = it } != -1) {
      totalSize += length
    }

    return totalSize
  }
}
