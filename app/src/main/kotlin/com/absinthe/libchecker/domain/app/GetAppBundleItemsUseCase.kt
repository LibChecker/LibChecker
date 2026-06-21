package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.STRING_ABI_MAP
import java.io.File
import java.util.Locale

class GetAppBundleItemsUseCase {

  private val localeCodes by lazy { Locale.getISOLanguages().toSet() }

  operator fun invoke(packageInfo: PackageInfo): List<AppBundleSplitItem> {
    return PackageUtils.getSplitsSourceDir(packageInfo)
      ?.map { split ->
        val name = split.substringAfterLast(File.separator)
        val middleName = name.removeSurrounding("split_config.", ".apk")
        AppBundleSplitItem(
          name = name,
          size = FileUtils.getFileSize(split),
          kind = when {
            STRING_ABI_MAP.keys.any { arch -> middleName.contains(arch) } ->
              AppBundleSplitKind.NativeLibs

            middleName.endsWith("dpi") -> AppBundleSplitKind.Materials

            localeCodes.contains(middleName) -> AppBundleSplitKind.Strings

            else -> AppBundleSplitKind.Others
          }
        )
      }
      .orEmpty()
  }
}

data class AppBundleSplitItem(
  val name: String,
  val size: Long,
  val kind: AppBundleSplitKind
)

enum class AppBundleSplitKind {
  NativeLibs,
  Materials,
  Strings,
  Others
}
