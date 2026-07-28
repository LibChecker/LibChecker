package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED

data class SnapshotDetailCountRenderState(
  val diffType: Int,
  @DrawableRes val iconRes: Int,
  val countText: String,
  @ColorRes val colorRes: Int
)

fun buildSnapshotDetailSignedCountText(diffType: Int, countText: String): String {
  return when (diffType) {
    ADDED -> "+$countText"
    REMOVED -> "−$countText"
    CHANGED -> "~$countText"
    MOVED -> "↔$countText"
    else -> countText
  }
}
