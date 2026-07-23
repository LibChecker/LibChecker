package com.absinthe.libchecker.domain.app.detail.content

import android.content.Context
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppBundleItem
import com.absinthe.libchecker.domain.app.detail.model.buildAppBundleItemDescription
import com.absinthe.libchecker.utils.extensions.sizeToString

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
