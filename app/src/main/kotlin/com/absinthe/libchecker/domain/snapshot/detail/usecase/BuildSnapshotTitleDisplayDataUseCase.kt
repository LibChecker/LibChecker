package com.absinthe.libchecker.domain.snapshot.detail.usecase

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailDiffTextStyle
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData
import com.absinthe.libchecker.domain.snapshot.detail.model.emphasizeSnapshotDetailDiffArrows
import com.absinthe.libchecker.domain.snapshot.display.buildSnapshotVersionDisplayDiff
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.sizeToString

class BuildSnapshotTitleDisplayDataUseCase(
  private val context: Context
) {

  operator fun invoke(request: Request): SnapshotTitleDisplayData {
    val item = request.item
    val isNewOrDeleted = item.deleted || item.newInstalled
    return SnapshotTitleDisplayData(
      appName = buildDiffText(item.labelDiff, isNewOrDeleted, request.diffTextStyle),
      packageName = buildPackageName(item, request.formatSplitPackageName),
      versionInfo = LCAppUtils.getDiffString(
        diff = item.buildSnapshotVersionDisplayDiff(context.getString(R.string.snapshot_version_archived)),
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffColor = request.diffTextStyle.highlightColor,
        emphasizeDiffs = request.diffTextStyle.emphasizeDiffs
      ).emphasizeSnapshotDetailDiffArrows(request.diffTextStyle.arrowColor),
      packageSize = buildPackageSize(item, isNewOrDeleted, request.diffTextStyle),
      apis = buildApis(item, isNewOrDeleted, request.diffTextStyle)
    )
  }

  private fun <T> buildDiffText(
    diff: SnapshotDiffItem.DiffNode<T>,
    isNewOrDeleted: Boolean,
    style: SnapshotDetailDiffTextStyle
  ): CharSequence {
    return LCAppUtils.getDiffString(
      diff = diff,
      isNewOrDeleted = isNewOrDeleted,
      highlightDiffColor = style.highlightColor,
      emphasizeDiffs = style.emphasizeDiffs
    ).emphasizeSnapshotDetailDiffArrows(style.arrowColor)
  }

  private fun buildPackageName(item: SnapshotDiffItem, formatSplitPackageName: Boolean): String {
    if (!formatSplitPackageName) {
      return item.packageName
    }

    val packageSplits = item.packageName.split("/")
    val first = packageSplits[0]
    val second = packageSplits.getOrNull(1)
    return if (second != null && second != first) "$first $ARROW $second" else first
  }

  private fun buildPackageSize(
    item: SnapshotDiffItem,
    isNewOrDeleted: Boolean,
    style: SnapshotDetailDiffTextStyle
  ): SnapshotTitlePackageSizeData? {
    if (item.packageSizeDiff.old <= 0L) {
      return null
    }

    val sizeDiff = SnapshotDiffItem.DiffNode(
      item.packageSizeDiff.old.sizeToString(context),
      item.packageSizeDiff.new?.sizeToString(context)
    )
    val text = SpannableStringBuilder(
      buildDiffText(sizeDiff, isNewOrDeleted, style)
    )
    var breakStart = -1
    if (item.packageSizeDiff.new != null) {
      val diffSize = item.packageSizeDiff.new - item.packageSizeDiff.old
      if (diffSize != 0L) {
        text.append(" ")
        breakStart = text.length
        text.append(if (diffSize > 0) "+" else "")
        text.append(diffSize.sizeToString(context))
      }
    }

    return SnapshotTitlePackageSizeData(
      text = text,
      breakStart = breakStart
    )
  }

  private fun buildApis(
    item: SnapshotDiffItem,
    isNewOrDeleted: Boolean,
    style: SnapshotDetailDiffTextStyle
  ): CharSequence {
    val targetDiff = buildDiffText(item.targetApiDiff, isNewOrDeleted, style).takeIf { item.targetApiDiff.old > 0 }
    val minDiff = buildDiffText(item.minSdkDiff, isNewOrDeleted, style).takeIf { item.minSdkDiff.old > 0 }
    val compileDiff = buildDiffText(item.compileSdkDiff, isNewOrDeleted, style).takeIf { item.compileSdkDiff.old > 0 }

    return buildSpannedString {
      targetDiff?.let {
        scale(1f) {
          append("Target: ")
        }
        append(it)
        append("  ")
      }

      minDiff?.let {
        scale(1f) {
          append("Min: ")
        }
        append(it)
        append("  ")
      }

      compileDiff?.let {
        scale(1f) {
          append("Compile: ")
        }
        append(it)
      }
    }
  }

  data class Request(
    val item: SnapshotDiffItem,
    val formatSplitPackageName: Boolean,
    val diffTextStyle: SnapshotDetailDiffTextStyle
  )

  private companion object {
    const val ARROW = "→"
  }
}
