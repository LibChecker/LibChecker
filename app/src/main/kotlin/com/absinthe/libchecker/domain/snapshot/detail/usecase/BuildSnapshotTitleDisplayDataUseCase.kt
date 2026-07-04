package com.absinthe.libchecker.domain.snapshot.detail.usecase

import android.content.Context
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData
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
      appName = LCAppUtils.getDiffString(item.labelDiff, isNewOrDeleted),
      packageName = buildPackageName(item, request.formatSplitPackageName),
      versionInfo = LCAppUtils.getDiffString(
        diff1 = item.versionNameDiff,
        diff2 = item.versionCodeDiff,
        isNewOrDeleted = isNewOrDeleted
      ),
      packageSize = buildPackageSize(item, isNewOrDeleted),
      apis = buildApis(item, isNewOrDeleted)
    )
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
    isNewOrDeleted: Boolean
  ): SnapshotTitlePackageSizeData? {
    if (item.packageSizeDiff.old <= 0L) {
      return null
    }

    val sizeDiff = SnapshotDiffItem.DiffNode(
      item.packageSizeDiff.old.sizeToString(context),
      item.packageSizeDiff.new?.sizeToString(context)
    )
    val text = StringBuilder(LCAppUtils.getDiffString(sizeDiff, isNewOrDeleted))
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
    isNewOrDeleted: Boolean
  ): CharSequence {
    val targetDiff = LCAppUtils.getDiffString(item.targetApiDiff, isNewOrDeleted).takeIf { item.targetApiDiff.old > 0 }
    val minDiff = LCAppUtils.getDiffString(item.minSdkDiff, isNewOrDeleted).takeIf { item.minSdkDiff.old > 0 }
    val compileDiff = LCAppUtils.getDiffString(item.compileSdkDiff, isNewOrDeleted).takeIf { item.compileSdkDiff.old > 0 }

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
    val formatSplitPackageName: Boolean
  )

  private companion object {
    const val ARROW = "→"
  }
}
