package com.absinthe.libchecker.domain.app.detail.action

import android.content.Intent
import java.io.File

class PrepareAppPackageShareActionUseCase(
  private val prepareAppPackageShareFileUseCase: PrepareAppPackageShareFileUseCase
) {

  suspend operator fun invoke(
    cacheDir: File,
    packageName: String
  ): AppPackageShareAction {
    val shareFile = prepareAppPackageShareFileUseCase(cacheDir, packageName)
    return AppPackageShareAction(
      shareFile = shareFile,
      shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = shareFile.mimeType
        putExtra(Intent.EXTRA_STREAM, shareFile.contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      },
      exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = shareFile.mimeType
        putExtra(Intent.EXTRA_TITLE, shareFile.file.name)
      }
    )
  }
}

data class AppPackageShareAction(
  val shareFile: AppPackageShareFile,
  val shareIntent: Intent,
  val exportIntent: Intent
)
