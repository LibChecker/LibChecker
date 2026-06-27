package com.absinthe.libchecker.domain.app.list.export

import android.content.pm.PackageInfo
import com.absinthe.libchecker.utils.PackageUtils
import timber.log.Timber

class BuildAppExportNativeLibrariesUseCase {

  operator fun invoke(packageInfo: PackageInfo): List<AppExportNativeLibrary> {
    val sourceLibs = runCatching {
      PackageUtils.getSourceLibs(
        packageInfo = packageInfo,
        includeNativeLibsDir = true,
        parseElf = false
      )
    }.onFailure {
      Timber.w(it, "Failed to read native libraries: ${packageInfo.packageName}")
    }.getOrDefault(emptyMap())

    return sourceLibs.flatMap { (abi, libs) ->
      libs.map { item ->
        AppExportNativeLibrary(
          abi = abi,
          name = item.name,
          path = "lib/$abi/${item.name}",
          size = item.size
        )
      }
    }.sortedWith(compareBy<AppExportNativeLibrary> { it.abi }.thenBy { it.name }.thenBy { it.path })
  }
}

data class AppExportNativeLibrary(
  val abi: String,
  val name: String,
  val path: String,
  val size: Long
)
