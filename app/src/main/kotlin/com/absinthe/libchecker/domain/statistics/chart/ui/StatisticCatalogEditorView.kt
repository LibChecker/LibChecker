package com.absinthe.libchecker.domain.statistics.chart.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCatalogEditorState
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class StatisticCatalogEditorView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private var onAction: (StatisticCatalogEditorAction) -> Unit = {}
  private val editorAdapter = StatisticCatalogEditorAdapter(context) { action ->
    onAction(action)
  }
  private val itemTouchHelper = ItemTouchHelper(
    StatisticItemTouchCallback(editorAdapter)
  )

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.chart_statistics_edit)
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
    layoutManager = LinearLayoutManager(context)
    adapter = editorAdapter
    overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    setPadding(0, 4.dp, 0, 24.dp)
  }

  init {
    orientation = VERTICAL
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    setPadding(0, 16.dp, 0, 0)
    addView(header)
    addView(list)
    itemTouchHelper.attachToRecyclerView(list)
    editorAdapter.startDrag = itemTouchHelper::startDrag
  }

  fun bind(
    state: StatisticCatalogEditorState,
    onAction: (StatisticCatalogEditorAction) -> Unit
  ) {
    this.onAction = onAction
    editorAdapter.bind(state)
  }

  override fun getHeaderView(): BottomSheetHeaderView = header
}

sealed interface StatisticCatalogEditorAction {
  data class Add(val statistic: StatisticDefinition) : StatisticCatalogEditorAction

  data class Remove(val statisticId: String) : StatisticCatalogEditorAction

  data class Move(val fromIndex: Int, val toIndex: Int) : StatisticCatalogEditorAction
}

private sealed interface StatisticCatalogEditorRow {
  data class Section(val title: String) : StatisticCatalogEditorRow

  data class Selected(
    val statistic: StatisticDefinition,
    val selectedIndex: Int,
    val selectedCount: Int
  ) : StatisticCatalogEditorRow

  data class Addable(val statistic: StatisticDefinition) : StatisticCatalogEditorRow

  data class Status(val text: String, val showProgress: Boolean = false) : StatisticCatalogEditorRow
}

private class StatisticCatalogEditorAdapter(
  private val context: Context,
  private val onAction: (StatisticCatalogEditorAction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  var startDrag: (RecyclerView.ViewHolder) -> Unit = {}
  private val rows = mutableListOf<StatisticCatalogEditorRow>()

  init {
    setHasStableIds(true)
  }

  fun bind(state: StatisticCatalogEditorState) {
    rows.clear()
    rows += StatisticCatalogEditorRow.Section(context.getString(R.string.chart_statistics_added))
    state.selectedStatistics.forEachIndexed { index, statistic ->
      rows += StatisticCatalogEditorRow.Selected(
        statistic = statistic,
        selectedIndex = index,
        selectedCount = state.selectedStatistics.size
      )
    }
    rows += StatisticCatalogEditorRow.Section(context.getString(R.string.chart_statistics_available))
    state.addableStatistics.forEach { statistic ->
      rows += StatisticCatalogEditorRow.Addable(statistic)
    }
    when {
      state.isRefreshing -> rows += StatisticCatalogEditorRow.Status(
        text = context.getString(R.string.chart_statistics_loading_online),
        showProgress = true
      )

      state.refreshFailed -> rows += StatisticCatalogEditorRow.Status(
        context.getString(R.string.chart_statistics_online_error)
      )

      state.addableStatistics.isEmpty() -> rows += StatisticCatalogEditorRow.Status(
        context.getString(R.string.chart_statistics_no_available)
      )
    }
    notifyDataSetChanged()
  }

  override fun getItemId(position: Int): Long {
    return when (val row = rows[position]) {
      is StatisticCatalogEditorRow.Section -> "section:${row.title}".hashCode().toLong()
      is StatisticCatalogEditorRow.Selected -> "selected:${row.statistic.id}".hashCode().toLong()
      is StatisticCatalogEditorRow.Addable -> "addable:${row.statistic.id}".hashCode().toLong()
      is StatisticCatalogEditorRow.Status -> "status:${row.text}".hashCode().toLong()
    }
  }

  override fun getItemViewType(position: Int): Int {
    return when (rows[position]) {
      is StatisticCatalogEditorRow.Section -> VIEW_TYPE_SECTION
      is StatisticCatalogEditorRow.Selected -> VIEW_TYPE_SELECTED
      is StatisticCatalogEditorRow.Addable -> VIEW_TYPE_ADDABLE
      is StatisticCatalogEditorRow.Status -> VIEW_TYPE_STATUS
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return when (viewType) {
      VIEW_TYPE_SECTION -> SectionViewHolder(SectionView(context))
      VIEW_TYPE_SELECTED, VIEW_TYPE_ADDABLE -> StatisticViewHolder(StatisticEditorItemView(context))
      else -> StatusViewHolder(StatusView(context))
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (val row = rows[position]) {
      is StatisticCatalogEditorRow.Section -> (holder as SectionViewHolder).view.bind(row.title)

      is StatisticCatalogEditorRow.Selected -> (holder as StatisticViewHolder).view.bindSelected(
        row = row,
        onRemove = { onAction(StatisticCatalogEditorAction.Remove(row.statistic.id)) },
        onStartDrag = { startDrag(holder) },
        onDragHandleTap = { context.showToast(R.string.chart_statistics_drag_hint) }
      )

      is StatisticCatalogEditorRow.Addable -> (holder as StatisticViewHolder).view.bindAddable(
        row.statistic
      ) {
        onAction(StatisticCatalogEditorAction.Add(row.statistic))
      }

      is StatisticCatalogEditorRow.Status -> (holder as StatusViewHolder).view.bind(row)
    }
  }

  override fun getItemCount(): Int = rows.size

  fun isSelected(position: Int): Boolean = rows.getOrNull(position) is StatisticCatalogEditorRow.Selected

  fun moveSelected(fromPosition: Int, toPosition: Int): Boolean {
    val from = rows.getOrNull(fromPosition) as? StatisticCatalogEditorRow.Selected ?: return false
    val to = rows.getOrNull(toPosition) as? StatisticCatalogEditorRow.Selected ?: return false
    rows.add(toPosition, rows.removeAt(fromPosition))
    notifyItemMoved(fromPosition, toPosition)
    onAction(StatisticCatalogEditorAction.Move(from.selectedIndex, to.selectedIndex))
    return true
  }

  private companion object {
    const val VIEW_TYPE_SECTION = 0
    const val VIEW_TYPE_SELECTED = 1
    const val VIEW_TYPE_ADDABLE = 2
    const val VIEW_TYPE_STATUS = 3
  }
}

private class StatisticItemTouchCallback(
  private val adapter: StatisticCatalogEditorAdapter
) : ItemTouchHelper.Callback() {
  override fun isLongPressDragEnabled(): Boolean = false

  override fun getMovementFlags(
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder
  ): Int {
    val dragFlags = if (adapter.isSelected(viewHolder.bindingAdapterPosition)) {
      ItemTouchHelper.UP or ItemTouchHelper.DOWN
    } else {
      0
    }
    return makeMovementFlags(dragFlags, 0)
  }

  override fun onMove(
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    target: RecyclerView.ViewHolder
  ): Boolean {
    return adapter.moveSelected(
      viewHolder.bindingAdapterPosition,
      target.bindingAdapterPosition
    )
  }

  override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
}

private class SectionView(context: Context) : TextView(context) {
  init {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 48.dp)
    gravity = Gravity.CENTER_VERTICAL
    setPadding(24.dp, 0, 24.dp, 0)
    setTypeface(typeface, Typeface.BOLD)
    setTextColor(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary))
  }

  fun bind(title: String) {
    text = title
  }
}

private class StatusView(context: Context) : LinearLayout(context) {
  private val progress = com.google.android.material.progressindicator.CircularProgressIndicator(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp).also { it.marginEnd = 12.dp }
    isIndeterminate = true
  }
  private val label = TextView(context).apply {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
  }

  init {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 64.dp)
    gravity = Gravity.CENTER_VERTICAL
    setPadding(24.dp, 0, 24.dp, 0)
    addView(progress)
    addView(label)
  }

  fun bind(row: StatisticCatalogEditorRow.Status) {
    progress.isVisible = row.showProgress
    label.text = row.text
  }
}

private class StatisticEditorItemView(context: Context) : LinearLayout(context) {
  private val dragTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private var dragDownX = 0f
  private var dragDownY = 0f
  private var isDragging = false
  private val icon = ImageView(context).apply {
    layoutParams = LayoutParams(40.dp, 40.dp).also { it.marginEnd = 8.dp }
    scaleType = ImageView.ScaleType.CENTER_INSIDE
  }
  private val labels = LinearLayout(context).apply {
    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
    orientation = VERTICAL
  }
  private val title = TextView(context).apply {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTypeface(typeface, Typeface.BOLD)
    textSize = 15f
    isSingleLine = true
  }
  private val subtitle = TextView(context).apply {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    textSize = 13f
    isSingleLine = true
  }
  private val action = AppCompatImageButton(context).apply {
    layoutParams = LayoutParams(48.dp, 48.dp)
    setBackgroundResource(R.drawable.ripple_chart_statistic_action_48dp)
  }
  private val dragHandle = AppCompatImageButton(context).apply {
    layoutParams = LayoutParams(48.dp, 48.dp)
    setBackgroundResource(R.drawable.ripple_chart_statistic_action_48dp)
    setImageResource(R.drawable.ic_drag_indicator)
    contentDescription = context.getString(R.string.chart_statistics_drag)
  }

  init {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 56.dp)
    orientation = HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    setPadding(16.dp, 0, 8.dp, 0)
    setBackgroundResource(
      context.getResourceIdByAttr(android.R.attr.selectableItemBackground)
    )
    labels.addView(title)
    labels.addView(subtitle)
    addView(icon)
    addView(labels)
    addView(action)
    addView(dragHandle)
  }

  fun bindSelected(
    row: StatisticCatalogEditorRow.Selected,
    onRemove: () -> Unit,
    onStartDrag: () -> Unit,
    onDragHandleTap: () -> Unit
  ) {
    bindStatistic(row.statistic)
    action.isVisible = true
    action.isEnabled = row.selectedCount > 1
    action.alpha = if (action.isEnabled) 1f else 0.38f
    action.setImageResource(R.drawable.ic_delete_outline)
    action.contentDescription = context.getString(
      R.string.chart_statistics_remove_description,
      row.statistic.title.resolve(context)
    )
    action.setOnClickListener { onRemove() }
    dragHandle.isVisible = true
    dragHandle.setOnClickListener { onDragHandleTap() }
    dragHandle.setOnTouchListener { view, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          dragDownX = event.x
          dragDownY = event.y
          isDragging = false
          view.parent.requestDisallowInterceptTouchEvent(true)
        }

        MotionEvent.ACTION_MOVE -> {
          val movedBeyondTouchSlop =
            kotlin.math.abs(event.x - dragDownX) > dragTouchSlop ||
              kotlin.math.abs(event.y - dragDownY) > dragTouchSlop
          if (!isDragging && movedBeyondTouchSlop) {
            isDragging = true
            view.isPressed = false
            view.parent.requestDisallowInterceptTouchEvent(false)
            onStartDrag()
          }
        }

        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          val wasDragging = isDragging
          view.parent.requestDisallowInterceptTouchEvent(false)
          isDragging = false
          return@setOnTouchListener wasDragging
        }
      }
      isDragging
    }
  }

  fun bindAddable(statistic: StatisticDefinition, onAdd: () -> Unit) {
    bindStatistic(statistic)
    action.isVisible = true
    action.isEnabled = true
    action.alpha = 1f
    action.setImageResource(R.drawable.ic_add)
    action.contentDescription = context.getString(
      R.string.chart_statistics_add_description,
      statistic.title.resolve(context)
    )
    action.setOnClickListener { onAdd() }
    dragHandle.isVisible = false
    dragHandle.setOnClickListener(null)
    dragHandle.setOnTouchListener(null)
  }

  private fun bindStatistic(statistic: StatisticDefinition) {
    icon.loadStatisticIcon(statistic.icon, selected = false)
    title.text = statistic.title.resolve(context)
    subtitle.text = context.getString(
      if (statistic.source == StatisticSource.BUILTIN) {
        R.string.chart_statistics_source_builtin
      } else {
        R.string.chart_statistics_source_online
      }
    )
  }
}

private class SectionViewHolder(val view: SectionView) : RecyclerView.ViewHolder(view)

private class StatisticViewHolder(val view: StatisticEditorItemView) : RecyclerView.ViewHolder(view)

private class StatusViewHolder(val view: StatusView) : RecyclerView.ViewHolder(view)
