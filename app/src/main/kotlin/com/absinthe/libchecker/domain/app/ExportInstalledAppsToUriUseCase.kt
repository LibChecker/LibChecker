package com.absinthe.libchecker.domain.app

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportInstalledAppsToUriUseCase(
  private val context: Context,
  private val contentResolver: ContentResolver,
  private val installedAppRepository: InstalledAppRepository,
  private val buildAppExportNativeLibraries: BuildAppExportNativeLibrariesUseCase
) {

  suspend operator fun invoke(
    uri: Uri,
    progress: suspend (Int) -> Unit
  ): Result = withContext(Dispatchers.IO) {
    val outputStream = contentResolver.openOutputStream(uri)
      ?: error("Unable to open output stream")
    val exportResult = outputStream.use { stream ->
      LcAppsExporter.export(
        context = context,
        installedAppRepository = installedAppRepository,
        buildAppExportNativeLibraries = buildAppExportNativeLibraries,
        outputStream = stream,
        progress = progress
      )
    }
    Result(exportResult.appCount)
  }

  data class Result(
    val appCount: Int
  )
}
