package com.absinthe.libchecker.domain.snapshot.list.ui.adapter

import android.graphics.Color
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.SnapshotListDisplayOptions
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotItemView
import com.absinthe.libchecker.domain.snapshot.list.usecase.stableSnapshotDiffItemIdFor
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.setAlphaForAll
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
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
    (holder.itemView as SnapshotItemView).apply {
      if (cardMode == CardMode.DEMO || cardMode == CardMode.GET_APP_UPDATE) {
        setSmoothRoundCorner(16.dp)
        strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
      } else {
        strokeColor = Color.TRANSPARENT
        radius = 0f
      }
    }
    (holder.itemView as SnapshotItemView).container.apply {
      when (val iconSource = packageIconSources[item.packageName]) {
        is SnapshotPackageIconSource.InstalledPackage -> icon.load(iconSource.packageInfo)

        SnapshotPackageIconSource.Fallback,
        null -> icon.load(R.drawable.ic_icon_blueprint)
      }

      if (item.deleted) {
        setAlphaForAll(0.7f)
      } else {
        setAlphaForAll(1.0f)
      }

      val isNewOrDeleted = item.deleted || item.newInstalled
      val highlightDiffColor = if (displayOptions.highlightDiffs) {
        context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
      } else {
        null
      }

      stateIndicator.apply {
        if (cardMode == CardMode.DEMO) {
          startDemoAnimation()
        } else {
          stopDemoAnimation()
          if (isNewOrDeleted) {
            added = false
            removed = false
            changed = false
            moved = false
          } else {
            setSnapshotStateCounts(item.added, item.removed, item.changed, item.moved)
          }
        }
      }

      setAppNameDisplay(
        labelDiff = item.labelDiff,
        isTrackItem = item.isTrackItem,
        packageStateLabel = when {
          item.newInstalled -> SnapshotItemView.PackageStateLabel.New
          item.deleted -> SnapshotItemView.PackageStateLabel.Deleted
          else -> null
        },
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffColor = highlightDiffColor,
        highlightText = highlightText
      )

      setPackageNameDisplay(item.packageName, highlightText)
      setVersionDisplay(
        versionNameDiff = item.versionNameDiff,
        versionCodeDiff = item.versionCodeDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffColor = highlightDiffColor
      )

      setPackageSizeDisplay(
        packageSizeDiff = item.packageSizeDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffColor = highlightDiffColor
      )

      setApiDisplay(
        targetApiDiff = item.targetApiDiff,
        minSdkDiff = item.minSdkDiff,
        compileSdkDiff = item.compileSdkDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffColor = highlightDiffColor
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
      (holder.itemView as SnapshotItemView).setItemContentDescription(
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
