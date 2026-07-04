package com.absinthe.libchecker.domain.snapshot.album.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class AlbumItemDisplayData(
  @DrawableRes val iconRes: Int,
  @ColorRes val iconBackgroundColorRes: Int,
  val title: CharSequence,
  val subtitle: CharSequence,
  val contentDescription: String,
  val action: AlbumItemAction
)

enum class AlbumItemAction {
  Comparison,
  Management,
  BackupRestore,
  Track
}

fun buildAlbumItemDescription(
  title: CharSequence,
  subtitle: CharSequence
): String {
  return listOf(title, subtitle)
    .mapNotNull { it.toString().trim().takeIf(String::isNotEmpty) }
    .joinToString()
}
