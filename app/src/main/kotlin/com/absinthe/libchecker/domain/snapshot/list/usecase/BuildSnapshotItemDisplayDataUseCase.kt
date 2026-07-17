package com.absinthe.libchecker.domain.snapshot.list.usecase

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeText
import com.absinthe.libchecker.domain.snapshot.display.buildSnapshotVersionDisplayDiff
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemAppNameDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemPackageSizeDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemPackageStateLabel
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemStateIndicatorData
import com.absinthe.libchecker.domain.snapshot.list.model.buildSnapshotItemAbiDescription
import com.absinthe.libchecker.domain.snapshot.list.model.buildSnapshotItemDescription
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.sizeToString
import java.util.Locale
import kotlin.math.abs

class BuildSnapshotItemDisplayDataUseCase(
  private val context: Context,
  private val buildSnapshotAbiDisplayData: BuildSnapshotAbiDisplayDataUseCase,
  private val buildSnapshotUpdateTimeDisplayData: BuildSnapshotUpdateTimeDisplayDataUseCase
) {

  operator fun invoke(request: Request): SnapshotItemDisplayData {
    val item = request.item
    val isNewOrDeleted = item.newInstalled || item.deleted
    val appName = SnapshotItemAppNameDisplayData(
      text = LCAppUtils.getDiffString(
        diff = item.labelDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffColor = request.highlightDiffColor,
        emphasizeDiffs = request.emphasizeDiffs
      ),
      showTrackIcon = item.isTrackItem,
      packageStateLabel = item.packageStateLabel
    )
    val stateIndicator = SnapshotItemStateIndicatorData(
      added = item.added,
      removed = item.removed,
      changed = item.changed,
      moved = item.moved,
      stateDescription = buildSnapshotStateDescription(
        added = item.added,
        removed = item.removed,
        changed = item.changed,
        moved = item.moved
      ),
      animate = request.animateStateIndicator
    )
    val versionInfo = LCAppUtils.getDiffString(
      diff = item.buildSnapshotVersionDisplayDiff(context.getString(R.string.snapshot_version_archived)),
      isNewOrDeleted = isNewOrDeleted,
      highlightDiffColor = request.highlightDiffColor,
      emphasizeDiffs = request.emphasizeDiffs
    )
    val packageSize = buildPackageSizeDisplayData(
      packageSizeDiff = item.packageSizeDiff,
      isNewOrDeleted = isNewOrDeleted,
      highlightDiffColor = request.highlightDiffColor,
      emphasizeDiffs = request.emphasizeDiffs
    )
    val apiText = buildApiText(
      item = item,
      isNewOrDeleted = isNewOrDeleted,
      highlightDiffColor = request.highlightDiffColor,
      emphasizeDiffs = request.emphasizeDiffs
    )
    val abi = SnapshotItemAbiDisplayData(
      abiDisplayData = buildSnapshotAbiDisplayData(item.abiDiff),
      showChangedAbi = item.abiDiff.new != null && item.abiDiff.old != item.abiDiff.new,
      tintChangedAbiBadge = request.tintChangedAbiBadge
    )
    val updateTimeDisplayData = buildSnapshotUpdateTimeDisplayData(
      BuildSnapshotUpdateTimeDisplayDataUseCase.Request(
        updateTime = item.updateTime,
        isVisible = request.showUpdateTime,
        isApexPackage = request.isApexPackage
      )
    )
    return SnapshotItemDisplayData(
      cardPresentation = request.cardPresentation,
      iconSource = request.iconSource,
      packageName = item.packageName,
      appName = appName,
      isNewInstalled = item.newInstalled,
      isDeleted = item.deleted,
      stateIndicator = stateIndicator,
      versionInfo = versionInfo,
      packageSize = packageSize,
      apiText = apiText,
      abi = abi,
      updateTimeDisplayData = updateTimeDisplayData,
      highlightText = request.highlightText,
      contentDescription = buildSnapshotItemDescription(
        appName = appName.text,
        packageName = item.packageName,
        versionInfo = versionInfo,
        packageSize = packageSize?.text,
        apiText = apiText,
        abiText = buildSnapshotItemAbiDescription(abi),
        updateTime = updateTimeDisplayData?.toDisplayText(),
        stateDescription = stateIndicator.stateDescription
      )
    )
  }

  private fun buildPackageSizeDisplayData(
    packageSizeDiff: SnapshotDiffItem.DiffNode<Long>,
    isNewOrDeleted: Boolean,
    highlightDiffColor: Int?,
    emphasizeDiffs: Boolean
  ): SnapshotItemPackageSizeDisplayData? {
    if (packageSizeDiff.old <= 0L) {
      return null
    }

    val sizeDiff = SnapshotDiffItem.DiffNode(
      packageSizeDiff.old.sizeToString(context, showBytes = false),
      packageSizeDiff.new?.sizeToString(context, showBytes = false)
    )
    val bytesDiff = SnapshotDiffItem.DiffNode(
      packageSizeDiff.old,
      packageSizeDiff.new
    )
    var packageSizeBreakStart = -1
    val diffText = buildSpannedString {
      append(
        LCAppUtils.getDiffString(
          diff1 = sizeDiff,
          diff2 = bytesDiff,
          diff2Suffix = " Bytes",
          isNewOrDeleted = isNewOrDeleted,
          highlightDiffColor = highlightDiffColor,
          emphasizeDiffs = emphasizeDiffs
        )
      )

      if (packageSizeDiff.new != null) {
        val diffSize = packageSizeDiff.new - packageSizeDiff.old
        if (diffSize != 0L) {
          append(" ")
          packageSizeBreakStart = length
          append(buildPackageSizeChangeText(diffSize, packageSizeDiff.old))
        }
      }
    }

    return SnapshotItemPackageSizeDisplayData(
      text = diffText,
      breakStart = packageSizeBreakStart
    )
  }

  private fun buildApiText(
    item: SnapshotDiffItem,
    isNewOrDeleted: Boolean,
    highlightDiffColor: Int?,
    emphasizeDiffs: Boolean
  ): CharSequence {
    val targetDiff = buildApiDiffString(item.targetApiDiff, isNewOrDeleted, highlightDiffColor, emphasizeDiffs)
    val minDiff = buildApiDiffString(item.minSdkDiff, isNewOrDeleted, highlightDiffColor, emphasizeDiffs)
    val compileDiff = buildApiDiffString(item.compileSdkDiff, isNewOrDeleted, highlightDiffColor, emphasizeDiffs)

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

  private fun buildApiDiffString(
    diff: SnapshotDiffItem.DiffNode<Short>,
    isNewOrDeleted: Boolean,
    highlightDiffColor: Int?,
    emphasizeDiffs: Boolean
  ): CharSequence? {
    return LCAppUtils.getDiffString(
      diff = diff,
      isNewOrDeleted = isNewOrDeleted,
      highlightDiffColor = highlightDiffColor,
      emphasizeDiffs = emphasizeDiffs
    ).takeIf { diff.old > 0 }
  }

  private fun buildSnapshotStateDescription(
    added: Int,
    removed: Int,
    changed: Int,
    moved: Int
  ): String {
    return listOf(
      buildSnapshotStateDescriptionPart(R.string.snapshot_indicator_added, added),
      buildSnapshotStateDescriptionPart(R.string.snapshot_indicator_removed, removed),
      buildSnapshotStateDescriptionPart(R.string.snapshot_indicator_changed, changed),
      buildSnapshotStateDescriptionPart(R.string.snapshot_indicator_moved, moved)
    ).filterNotNull().joinToString()
  }

  private fun buildSnapshotStateDescriptionPart(
    @StringRes labelRes: Int,
    count: Int
  ): String? {
    return count.takeIf { it > 0 }?.let {
      "${context.getString(labelRes)} $it"
    }
  }

  private fun buildPackageSizeChangeText(diffSize: Long, oldSize: Long): String {
    return buildString {
      if (diffSize > 0) {
        append("+")
      }
      append(diffSize.sizeToString(context))
      append(", ")
      if (diffSize > 0) {
        append("+")
      }
      val percentage = diffSize.toFloat() / oldSize
      if (abs(percentage) < 0.001f) {
        if (percentage < 0) {
          append("-")
        }
        append("<0.1%")
      } else {
        append(String.format(Locale.getDefault(), "%.1f%%", percentage * 100))
      }
    }
  }

  private fun SnapshotUpdateTimeDisplayData.toDisplayText(): String {
    val displayText = when (val text = text) {
      SnapshotUpdateTimeText.Preinstalled -> context.getString(R.string.snapshot_preinstalled_app)

      is SnapshotUpdateTimeText.LastUpdated ->
        context.getString(R.string.format_last_updated).format(text.timeText)
    }
    return if (isApexPackage) {
      "$displayText, APEX"
    } else {
      displayText
    }
  }

  data class Request(
    val item: SnapshotDiffItem,
    val cardPresentation: SnapshotItemCardPresentation,
    val iconSource: SnapshotPackageIconSource?,
    val showUpdateTime: Boolean,
    val isApexPackage: Boolean,
    val animateStateIndicator: Boolean,
    val tintChangedAbiBadge: Boolean,
    val highlightDiffColor: Int?,
    val emphasizeDiffs: Boolean,
    val highlightText: String
  )
}

private val SnapshotDiffItem.packageStateLabel: SnapshotItemPackageStateLabel?
  get() = when {
    newInstalled -> SnapshotItemPackageStateLabel.New
    deleted -> SnapshotItemPackageStateLabel.Deleted
    else -> null
  }
