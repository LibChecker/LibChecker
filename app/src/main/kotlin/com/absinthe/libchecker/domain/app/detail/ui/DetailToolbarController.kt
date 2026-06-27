package com.absinthe.libchecker.domain.app.detail.ui

import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
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

  private val adapter = AppDetailToolbarAdapter()
  private var isToolbarCollapsed = false
  private val quicklyLaunchItem = AppDetailToolbarItem(
    icon = R.drawable.ic_launch,
    tooltipTextRes = R.string.further_operation,
    onClick = onQuickLaunchClick
  )
  private val processItem = AppDetailToolbarItem(
    icon = R.drawable.ic_processes,
    tooltipTextRes = R.string.menu_process,
    onClick = onProcessClick
  )
  private val offsetChangedListener = AppBarLayout.OnOffsetChangedListener { layout, verticalOffset ->
    isToolbarCollapsed = if (abs(verticalOffset) - layout.totalScrollRange == 0) {
      if (!isToolbarCollapsed) {
        addItemIfAbsent(quicklyLaunchItem)
      }
      true
    } else {
      if (isToolbarCollapsed) {
        removeItem(quicklyLaunchItem)
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
    addItemIfAbsent(
      AppDetailToolbarItem(R.drawable.ic_lib_sort, R.string.menu_sort, onSortClick)
    )
    if (showHarmonyToggle) {
      addItemIfAbsent(
        AppDetailToolbarItem(R.drawable.ic_harmonyos_logo, R.string.ability, onHarmonyToggle)
      )
    }
  }

  fun addCompareAction(onCompareClick: () -> Unit) {
    addItemIfAbsent(
      AppDetailToolbarItem(R.drawable.ic_compare, R.string.compare_with_current, onCompareClick)
    )
  }

  fun setProcessActionVisible(visible: Boolean) {
    if (visible) {
      addItemIfAbsent(processItem)
    } else {
      removeItem(processItem)
    }
  }

  fun updateProcessTooltip(@StringRes tooltipTextRes: Int) {
    if (processItem.tooltipTextRes != tooltipTextRes) {
      processItem.tooltipTextRes = tooltipTextRes
      adapter.data.indexOf(processItem).takeIf { it >= 0 }?.let {
        adapter.notifyItemChanged(it)
      }
    }
  }

  fun reset() {
    adapter.setList(emptyList())
    isToolbarCollapsed = false
    processItem.tooltipTextRes = R.string.menu_process
  }

  fun release() {
    appBarLayout.removeOnOffsetChangedListener(offsetChangedListener)
  }

  private fun addItemIfAbsent(item: AppDetailToolbarItem) {
    if (!adapter.data.contains(item)) {
      adapter.addData(item)
    }
  }

  private fun removeItem(item: AppDetailToolbarItem) {
    if (adapter.data.contains(item)) {
      adapter.remove(item)
    }
  }
}
