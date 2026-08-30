package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toItemViewRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toTitleRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailItemView
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailTitleView
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

sealed interface SnapshotDetailRow {
  data class Header(
    val section: SnapshotDetailSection,
    val expanded: Boolean
  ) : SnapshotDetailRow

  data class Item(
    @LibType val sectionType: Int,
    val displayData: SnapshotDetailItemDisplayData
  ) : SnapshotDetailRow
}

data class SnapshotDetailInteractionPolicy(
  val opensDetail: Boolean,
  val opensReference: Boolean,
  val referenceLabel: String?,
  val ruleChipLabel: String?,
  val ruleChipRegexName: String?
)

internal fun buildSnapshotDetailRows(
  sections: List<SnapshotDetailSection>,
  collapsedTypes: Set<Int> = emptySet()
): List<SnapshotDetailRow> {
  return buildList {
    sections.forEach { section ->
      val expanded = section.type !in collapsedTypes
      add(SnapshotDetailRow.Header(section = section, expanded = expanded))
      if (expanded) {
        section.items.forEach { item ->
          add(
            SnapshotDetailRow.Item(
              sectionType = section.type,
              displayData = item
            )
          )
        }
      }
    }
  }
}

internal fun buildSnapshotDetailVisibleReportText(rows: List<SnapshotDetailRow>): String {
  return buildString {
    rows.forEach { row ->
      when (row) {
        is SnapshotDetailRow.Header -> append(row.section.reportText)
        is SnapshotDetailRow.Item -> append(row.displayData.reportText)
      }
    }
  }
}

internal fun SnapshotDetailRow.Item.interactionPolicy(
  ownerPackageName: String
): SnapshotDetailInteractionPolicy {
  val item = displayData.item
  val opensReference = !(
    item.itemType == DEX ||
      (isComponentType(item.itemType) && item.name.startsWith(ownerPackageName))
    )
  return SnapshotDetailInteractionPolicy(
    opensDetail = item.diffType != REMOVED,
    opensReference = opensReference,
    referenceLabel = displayData.ruleChip?.label,
    ruleChipLabel = displayData.ruleChip?.label,
    ruleChipRegexName = displayData.ruleChip?.regexName
  )
}

class SnapshotDetailAdapter(
  private val onRuleChipClick: (SnapshotDetailItemDisplayData) -> Unit = {}
) : BaseQuickAdapter<SnapshotDetailRow, BaseViewHolder>() {

  fun submitSections(sections: List<SnapshotDetailSection>) {
    setList(buildSnapshotDetailRows(sections))
  }

  fun toggleSectionAt(position: Int): Boolean {
    val row = data.getOrNull(position) as? SnapshotDetailRow.Header ?: return false
    val childRows = row.section.items.map { displayData ->
      SnapshotDetailRow.Item(sectionType = row.section.type, displayData = displayData)
    }
    if (childRows.isEmpty()) {
      return false
    }
    data[position] = row.copy(expanded = !row.expanded)
    if (row.expanded) {
      removeAtRange((position + 1)..(position + childRows.size))
    } else {
      addData(position + 1, childRows)
    }
    return true
  }

  fun itemAt(position: Int): SnapshotDetailRow? = data.getOrNull(position)

  fun reportText(): String = buildSnapshotDetailVisibleReportText(data)

  override fun getItemViewType(position: Int, list: List<SnapshotDetailRow>): Int {
    return when (list[position]) {
      is SnapshotDetailRow.Header -> VIEW_TYPE_HEADER
      is SnapshotDetailRow.Item -> VIEW_TYPE_ITEM
    }
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    val itemView = when (viewType) {
      VIEW_TYPE_HEADER -> SnapshotDetailTitleView(parent.context)
      VIEW_TYPE_ITEM -> SnapshotDetailItemView(parent.context)
      else -> throw IllegalArgumentException("Unknown viewType: $viewType")
    }.also {
      it.layoutParams = RecyclerView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }
    return createBaseViewHolder(itemView)
  }

  override fun convert(holder: BaseViewHolder, item: SnapshotDetailRow) {
    when (item) {
      is SnapshotDetailRow.Header -> {
        (holder.itemView as SnapshotDetailTitleView).render(
          item.section.toTitleRenderState(expanded = item.expanded)
        )
      }

      is SnapshotDetailRow.Item -> {
        val itemView = holder.itemView as SnapshotDetailItemView
        itemView.render(item.displayData.toItemViewRenderState())
        val ruleChip = item.displayData.ruleChip
        if (ruleChip == null) {
          itemView.setChipOnClickListener(null)
        } else {
          itemView.setChipOnClickListener(
            View.OnClickListener { view ->
              if (AntiShakeUtils.isInvalidClick(view)) {
                return@OnClickListener
              }
              onRuleChipClick(item.displayData)
            }
          )
        }
      }
    }
  }

  private companion object {
    const val VIEW_TYPE_HEADER = 1
    const val VIEW_TYPE_ITEM = 2
  }
}
