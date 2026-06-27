package com.absinthe.libchecker.domain.snapshot.display

import android.content.Context
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.PackageUtils

class BuildSnapshotAbiDisplayDataUseCase(
  private val context: Context
) {

  operator fun invoke(abiDiff: SnapshotDiffItem.DiffNode<Short>): SnapshotAbiDisplayData {
    return SnapshotAbiDisplayData(
      old = buildAbiDisplayItem(abiDiff.old),
      new = abiDiff.new?.let(::buildAbiDisplayItem)
    )
  }

  private fun buildAbiDisplayItem(abi: Short): SnapshotAbiDisplayItem {
    val abiInt = abi.toInt()
    return SnapshotAbiDisplayItem(
      text = PackageUtils.getAbiString(context, abiInt, showExtraInfo = false),
      badgeRes = PackageUtils.getAbiBadgeResource(abiInt).takeIf {
        abiInt != Constants.ERROR && abiInt != Constants.OVERLAY && it != 0
      },
      isMultiArch = abi / Constants.MULTI_ARCH == 1
    )
  }
}

data class SnapshotAbiDisplayData(
  val old: SnapshotAbiDisplayItem,
  val new: SnapshotAbiDisplayItem?
)

data class SnapshotAbiDisplayItem(
  val text: String,
  @DrawableRes val badgeRes: Int?,
  val isMultiArch: Boolean
)
