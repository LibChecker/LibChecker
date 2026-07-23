package com.absinthe.libchecker.domain.app.detail.content

import android.content.Context
import android.content.pm.PackageInfo
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppBundleItem
import com.absinthe.libchecker.domain.app.detail.model.buildAppBundleItemDescription
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.STRING_ABI_MAP
import com.absinthe.libchecker.utils.extensions.sizeToString
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val localeCodes by lazy { Locale.getISOLanguages().toSet() }

internal suspend fun getAppBundleSplitItems(
  packageInfo: PackageInfo
): List<AppBundleSplitItem> = withContext(Dispatchers.IO) {
  PackageUtils.getSplitsSourceDir(packageInfo)
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

internal fun buildAppBundleItemDisplayData(
  context: Context,
  items: List<AppBundleSplitItem>
): List<AppBundleItem> {
  return items.map { item ->
    val sizeText = item.size.sizeToString(context, showBytes = false)
    AppBundleItem(
      iconRes = item.kind.toIconRes(),
      nameText = item.name,
      sizeText = sizeText,
      contentDescription = buildAppBundleItemDescription(item.name, sizeText),
      size = item.size
    )
  }
}

@DrawableRes
private fun AppBundleSplitKind.toIconRes(): Int {
  return when (this) {
    AppBundleSplitKind.NativeLibs -> R.drawable.ic_logo
    AppBundleSplitKind.Materials -> R.drawable.ic_outline_image
    AppBundleSplitKind.Strings -> R.drawable.ic_translate
    AppBundleSplitKind.Others -> R.drawable.ic_split
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
