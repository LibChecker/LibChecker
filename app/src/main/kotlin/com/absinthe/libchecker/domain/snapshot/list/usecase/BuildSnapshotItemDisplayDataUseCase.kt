package com.absinthe.libchecker.domain.snapshot.list.usecase

import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemApiDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemStateIndicatorData
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource

class BuildSnapshotItemDisplayDataUseCase(
  private val buildSnapshotAbiDisplayData: BuildSnapshotAbiDisplayDataUseCase,
  private val buildSnapshotUpdateTimeDisplayData: BuildSnapshotUpdateTimeDisplayDataUseCase
) {

  operator fun invoke(request: Request): SnapshotItemDisplayData {
    val item = request.item
    return SnapshotItemDisplayData(
      cardPresentation = request.cardPresentation,
      iconSource = request.iconSource,
      packageName = item.packageName,
      labelDiff = item.labelDiff,
      isTrackItem = item.isTrackItem,
      isNewInstalled = item.newInstalled,
      isDeleted = item.deleted,
      stateIndicator = SnapshotItemStateIndicatorData(
        added = item.added,
        removed = item.removed,
        changed = item.changed,
        moved = item.moved,
        animate = request.animateStateIndicator
      ),
      versionNameDiff = item.versionNameDiff,
      versionCodeDiff = item.versionCodeDiff,
      packageSizeDiff = item.packageSizeDiff,
      api = SnapshotItemApiDisplayData(
        targetApiDiff = item.targetApiDiff,
        minSdkDiff = item.minSdkDiff,
        compileSdkDiff = item.compileSdkDiff
      ),
      abi = SnapshotItemAbiDisplayData(
        abiDisplayData = buildSnapshotAbiDisplayData(item.abiDiff),
        showChangedAbi = item.abiDiff.new != null && item.abiDiff.old != item.abiDiff.new,
        tintChangedAbiBadge = request.tintChangedAbiBadge
      ),
      updateTimeDisplayData = buildSnapshotUpdateTimeDisplayData(
        BuildSnapshotUpdateTimeDisplayDataUseCase.Request(
          updateTime = item.updateTime,
          isVisible = request.showUpdateTime,
          isApexPackage = request.isApexPackage
        )
      ),
      highlightDiffs = request.highlightDiffs,
      highlightText = request.highlightText
    )
  }

  data class Request(
    val item: SnapshotDiffItem,
    val cardPresentation: SnapshotItemCardPresentation,
    val iconSource: SnapshotPackageIconSource?,
    val showUpdateTime: Boolean,
    val isApexPackage: Boolean,
    val animateStateIndicator: Boolean,
    val tintChangedAbiBadge: Boolean,
    val highlightDiffs: Boolean,
    val highlightText: String
  )
}
