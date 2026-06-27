package com.absinthe.libchecker.domain.settings.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class BuildLogShareIntentUseCase(
  private val context: Context,
  private val applicationId: String
) {

  suspend operator fun invoke(): Result<Intent?> = withContext(Dispatchers.IO) {
    runCatching {
      val logDir = File(context.cacheDir, LOG_DIR_NAME)
      if (!logDir.exists() || !logDir.isDirectory) {
        return@runCatching null
      }

      val latestLogFile = logDir.listFiles()
        ?.filter { it.isFile && it.name.endsWith(LOG_FILE_SUFFIX) }
        ?.maxByOrNull { it.lastModified() }
        ?: return@runCatching null

      Timber.d("Latest log file: ${latestLogFile.absolutePath}")
      val uri = FileProvider.getUriForFile(
        context,
        "$applicationId.fileprovider",
        latestLogFile
      )
      Intent(Intent.ACTION_SEND).apply {
        type = MIMETYPE_TEXT
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    }
  }

  private companion object {
    const val LOG_DIR_NAME = "logs"
    const val LOG_FILE_SUFFIX = ".log"
    const val MIMETYPE_TEXT = "text/plain"
  }
}
