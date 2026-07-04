package com.absinthe.libchecker.domain.snapshot.list.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.snapshot.SnapshotListDisplayOptions
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotItemView
import com.absinthe.libchecker.domain.snapshot.list.usecase.stableSnapshotDiffItemIdFor
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val ARROW = "→"
const val ARROW_REVERT = "←"
class SnapshotAdapter(
  private val buildSnapshotAbiDisplayData: BuildSnapshotAbiDisplayDataUseCase,
  private val buildSnapshotUpdateTimeDisplayData: BuildSnapshotUpdateTimeDisplayDataUseCase,
  private val cardMode: CardMode = CardMode.NORMAL
) : HighlightAdapter<SnapshotDiffItem>(SnapshotDiffUtil()) {

  init {
    setHasStableIds(true)
  }

  private var displayOptions = SnapshotListDisplayOptions()
  private var packageIconSources: Map<String, SnapshotPackageIconSource> = emptyMap()
  private var apexPackageNames: Set<String> = emptySet()

  fun setDisplayOptions(displayOptions: SnapshotListDisplayOptions) {
    this.displayOptions = displayOptions
  }

  fun setPackageIconSources(packageIconSources: Map<String, SnapshotPackageIconSource>) {
    this.packageIconSources = packageIconSources
  }

  fun setApexPackageNames(apexPackageNames: Set<String>) {
    this.apexPackageNames = apexPackageNames
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      SnapshotItemView(context).also {
        it.layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: SnapshotDiffItem) {
    val itemView = holder.itemView as SnapshotItemView
    itemView.setCardPresentation(cardMode.toCardPresentation())
    itemView.container.apply {
      val isNewOrDeleted = item.deleted || item.newInstalled

      setIconSource(packageIconSources[item.packageName])
      setDeleted(item.deleted)
      setStateIndicator(
        added = item.added,
        removed = item.removed,
        changed = item.changed,
        moved = item.moved,
        isNewOrDeleted = isNewOrDeleted,
        animate = cardMode == CardMode.DEMO
      )

      setAppNameDisplay(
        labelDiff = item.labelDiff,
        isTrackItem = item.isTrackItem,
        packageStateLabel = when {
          item.newInstalled -> SnapshotItemView.PackageStateLabel.New
          item.deleted -> SnapshotItemView.PackageStateLabel.Deleted
          else -> null
        },
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = displayOptions.highlightDiffs,
        highlightText = highlightText
      )

      setPackageNameDisplay(item.packageName, highlightText)
      setVersionDisplay(
        versionNameDiff = item.versionNameDiff,
        versionCodeDiff = item.versionCodeDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = displayOptions.highlightDiffs
      )

      setPackageSizeDisplay(
        packageSizeDiff = item.packageSizeDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = displayOptions.highlightDiffs
      )

      setApiDisplay(
        targetApiDiff = item.targetApiDiff,
        minSdkDiff = item.minSdkDiff,
        compileSdkDiff = item.compileSdkDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = displayOptions.highlightDiffs
      )

      val abiDisplayData = buildSnapshotAbiDisplayData(item.abiDiff)
      setAbiDisplay(
        abiDisplayData = abiDisplayData,
        showChangedAbi = item.abiDiff.new != null && item.abiDiff.old != item.abiDiff.new,
        tintChangedAbiBadge = displayOptions.tintAbiLabels
      )

      val updateTimeDisplayData = buildSnapshotUpdateTimeDisplayData(
        BuildSnapshotUpdateTimeDisplayDataUseCase.Request(
          updateTime = item.updateTime,
          isVisible = displayOptions.showUpdateTime && cardMode != CardMode.GET_APP_UPDATE,
          isApexPackage = item.packageName in apexPackageNames
        )
      )
      setUpdateTimeDisplay(updateTimeDisplayData)
      itemView.setItemContentDescription(
        added = item.added,
        removed = item.removed,
        changed = item.changed,
        moved = item.moved
      )
    }
  }

  override fun getItemId(position: Int): Long {
    if (position !in data.indices) {
      return Long.MIN_VALUE + position
    }
    return stableSnapshotDiffItemIdFor(data[position])
  }

  enum class CardMode {
    NORMAL,
    DEMO,
    GET_APP_UPDATE
  }
}

private fun SnapshotAdapter.CardMode.toCardPresentation(): SnapshotItemView.CardPresentation {
  return when (this) {
    SnapshotAdapter.CardMode.NORMAL -> SnapshotItemView.CardPresentation.Normal

    SnapshotAdapter.CardMode.DEMO,
    SnapshotAdapter.CardMode.GET_APP_UPDATE -> SnapshotItemView.CardPresentation.Rounded
  }
}
