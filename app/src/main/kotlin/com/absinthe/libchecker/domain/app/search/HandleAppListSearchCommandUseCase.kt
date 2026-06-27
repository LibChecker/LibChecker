package com.absinthe.libchecker.domain.app.search

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.list.export.ExportAppListUseCase
import com.absinthe.libchecker.domain.settings.repository.DeveloperSettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HandleAppListSearchCommandUseCase(
  private val developerSettingsRepository: DeveloperSettingsRepository
) {

  operator fun invoke(
    query: String,
    timestampMillis: Long = System.currentTimeMillis()
  ): Result {
    return when {
      query.equals(EASTER_EGG_QUERY, ignoreCase = true) -> {
        Result.EasterEgg
      }

      query == Constants.COMMAND_DEBUG_MODE -> {
        developerSettingsRepository.debugMode = true
        Result.DebugModeEnabled
      }

      query == Constants.COMMAND_USER_MODE -> {
        developerSettingsRepository.debugMode = false
        Result.UserModeEnabled
      }

      query == Constants.COMMAND_DUMP_APPS_INFO_TXT -> {
        Result.DumpAppsInfo(
          format = ExportAppListUseCase.Format.PlainText,
          fileName = buildDumpAppsInfoFileName(
            timestampMillis = timestampMillis,
            extension = "txt"
          )
        )
      }

      query == Constants.COMMAND_DUMP_APPS_INFO_MD -> {
        Result.DumpAppsInfo(
          format = ExportAppListUseCase.Format.Markdown,
          fileName = buildDumpAppsInfoFileName(
            timestampMillis = timestampMillis,
            extension = "md"
          )
        )
      }

      else -> {
        Result.None
      }
    }
  }

  private fun buildDumpAppsInfoFileName(
    timestampMillis: Long,
    extension: String
  ): String {
    val formattedTime = SimpleDateFormat(DUMP_TIMESTAMP_PATTERN, Locale.getDefault())
      .format(Date(timestampMillis))
    return "LibChecker-Dump-Apps-Info-$formattedTime.$extension"
  }

  sealed interface Result {
    data object None : Result
    data object EasterEgg : Result
    data object DebugModeEnabled : Result
    data object UserModeEnabled : Result

    data class DumpAppsInfo(
      val format: ExportAppListUseCase.Format,
      val fileName: String
    ) : Result
  }

  private companion object {
    private const val EASTER_EGG_QUERY = "Easter Egg"
    private const val DUMP_TIMESTAMP_PATTERN = "yyyy-MM-dd, HH:mm:ss"
  }
}
