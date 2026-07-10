package com.absinthe.libchecker.domain.app.detail.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppDetailToolbarAction
import com.absinthe.libchecker.domain.app.detail.model.AppDetailToolbarItem
import com.absinthe.libchecker.domain.app.detail.ui.adapter.AppDetailToolbarAdapter
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

class DetailToolbarController(
  private val toolbarView: RecyclerView,
  private val appBarLayout: AppBarLayout,
  private val onSortClick: () -> Unit,
  private val onQuickLaunchClick: () -> Unit,
  private val onProcessClick: () -> Unit
) {

  private val adapter = AppDetailToolbarAdapter(::onActionClick)
  private var isToolbarCollapsed = false
  private var onHarmonyToggle: (() -> Unit)? = null
  private var onCompareClick: (() -> Unit)? = null
  private val quicklyLaunchItem = createItem(
    action = AppDetailToolbarAction.QUICK_LAUNCH,
    iconRes = R.drawable.ic_launch,
    labelRes = R.string.further_operation
  )
  private var processItem = createItem(
    action = AppDetailToolbarAction.PROCESS,
    iconRes = R.drawable.ic_processes,
    labelRes = R.string.menu_process
  )
  private val offsetChangedListener = AppBarLayout.OnOffsetChangedListener { layout, verticalOffset ->
    isToolbarCollapsed = if (abs(verticalOffset) - layout.totalScrollRange == 0) {
      if (!isToolbarCollapsed) {
        addItemIfAbsent(quicklyLaunchItem)
      }
      true
    } else {
      if (isToolbarCollapsed) {
        removeItem(AppDetailToolbarAction.QUICK_LAUNCH)
      }
      false
    }
  }

  init {
    toolbarView.apply {
      adapter = this@DetailToolbarController.adapter
      layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
      itemAnimator = null
    }
    appBarLayout.addOnOffsetChangedListener(offsetChangedListener)
  }

  fun setupBaseActions(
    showHarmonyToggle: Boolean,
    onHarmonyToggle: () -> Unit
  ) {
    this.onHarmonyToggle = onHarmonyToggle
    addItemIfAbsent(
      createItem(AppDetailToolbarAction.SORT, R.drawable.ic_lib_sort, R.string.menu_sort)
    )
    if (showHarmonyToggle) {
      addItemIfAbsent(
        createItem(AppDetailToolbarAction.HARMONY_TOGGLE, R.drawable.ic_harmonyos_logo, R.string.ability)
      )
    }
  }

  fun addCompareAction(onCompareClick: () -> Unit) {
    this.onCompareClick = onCompareClick
    addItemIfAbsent(
      createItem(AppDetailToolbarAction.COMPARE, R.drawable.ic_compare, R.string.compare_with_current)
    )
  }

  fun setProcessActionVisible(visible: Boolean) {
    if (visible) {
      addItemIfAbsent(processItem)
    } else {
      removeItem(AppDetailToolbarAction.PROCESS)
    }
  }

  fun updateProcessTooltip(@StringRes tooltipTextRes: Int) {
    val label = toolbarView.context.getString(tooltipTextRes)
    if (processItem.label != label) {
      processItem = processItem.copy(label = label)
      replaceItem(processItem)
    }
  }

  fun reset() {
    adapter.setList(emptyList())
    isToolbarCollapsed = false
    onHarmonyToggle = null
    onCompareClick = null
    processItem = createItem(
      AppDetailToolbarAction.PROCESS,
      R.drawable.ic_processes,
      R.string.menu_process
    )
  }

  fun release() {
    appBarLayout.removeOnOffsetChangedListener(offsetChangedListener)
  }

  private fun addItemIfAbsent(item: AppDetailToolbarItem) {
    if (adapter.data.none { it.action == item.action }) {
      adapter.addData(item)
    }
  }

  private fun removeItem(action: AppDetailToolbarAction) {
    adapter.data.firstOrNull { it.action == action }?.let(adapter::remove)
  }

  private fun replaceItem(item: AppDetailToolbarItem) {
    adapter.data.indexOfFirst { it.action == item.action }
      .takeIf { it >= 0 }
      ?.let { position ->
        adapter.data[position] = item
        adapter.notifyItemChanged(position)
      }
  }

  private fun onActionClick(action: AppDetailToolbarAction) {
    when (action) {
      AppDetailToolbarAction.SORT -> onSortClick()
      AppDetailToolbarAction.QUICK_LAUNCH -> onQuickLaunchClick()
      AppDetailToolbarAction.PROCESS -> onProcessClick()
      AppDetailToolbarAction.HARMONY_TOGGLE -> onHarmonyToggle?.invoke()
      AppDetailToolbarAction.COMPARE -> onCompareClick?.invoke()
    }
  }

  private fun createItem(
    action: AppDetailToolbarAction,
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int
  ): AppDetailToolbarItem {
    return AppDetailToolbarItem(
      action = action,
      iconRes = iconRes,
      label = toolbarView.context.getString(labelRes)
    )
  }
}
