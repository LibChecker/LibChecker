package com.absinthe.libchecker.domain.snapshot.list.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.snapshot.SnapshotListDisplayOptions
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotItemView
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.stableSnapshotDiffItemIdFor
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val ARROW = "→"
const val ARROW_REVERT = "←"
class SnapshotAdapter(
  private val buildSnapshotItemDisplayData: BuildSnapshotItemDisplayDataUseCase,
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
    itemView.render(
      buildSnapshotItemDisplayData(
        BuildSnapshotItemDisplayDataUseCase.Request(
          item = item,
          cardPresentation = cardMode.toCardPresentation(),
          iconSource = packageIconSources[item.packageName],
          showUpdateTime = displayOptions.showUpdateTime && cardMode != CardMode.GET_APP_UPDATE,
          isApexPackage = item.packageName in apexPackageNames,
          animateStateIndicator = cardMode == CardMode.DEMO,
          tintChangedAbiBadge = displayOptions.tintAbiLabels,
          highlightDiffs = displayOptions.highlightDiffs,
          highlightText = highlightText
        )
      )
    )
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

private fun SnapshotAdapter.CardMode.toCardPresentation(): SnapshotItemCardPresentation {
  return when (this) {
    SnapshotAdapter.CardMode.NORMAL -> SnapshotItemCardPresentation.Normal

    SnapshotAdapter.CardMode.DEMO,
    SnapshotAdapter.CardMode.GET_APP_UPDATE -> SnapshotItemCardPresentation.Rounded
  }
}
