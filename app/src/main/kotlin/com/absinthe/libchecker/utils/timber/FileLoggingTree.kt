package com.absinthe.libchecker.utils.timber

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

class FileLoggingTree(context: Context) : Timber.DebugTree() {

  private val logFile: File

  init {
    val logDir = File(context.cacheDir, "logs")
    if (!logDir.exists()) {
      logDir.mkdir()
    }
    val logTimeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
    logFile = File(logDir, "$logTimeStamp.log")
    try {
      logFile.createNewFile()
    } catch (e: Exception) {
      Timber.e(e)
    }
    // Remove old log files, keeping the latest 3
    val logFiles = logDir.listFiles { file -> file.isFile && file.extension == "log" }
    logFiles?.sortedByDescending { it.lastModified() }?.drop(3)?.forEach { file ->
      try {
        file.delete()
      } catch (e: Exception) {
        Timber.e(e, "Failed to delete old log file: ${file.name}")
      }
    }
  }

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    val logTimeStamp =
      SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    val priorityChar = when (priority) {
      Log.VERBOSE -> "V"
      Log.DEBUG -> "D"
      Log.INFO -> "I"
      Log.WARN -> "W"
      Log.ERROR -> "E"
      Log.ASSERT -> "A"
      else -> "?"
    }

    logFile.appendText("$logTimeStamp $priorityChar/$tag: $message\n")
    t?.let {
      logFile.appendText(Log.getStackTraceString(it) + "\n")
    }
  }
}
