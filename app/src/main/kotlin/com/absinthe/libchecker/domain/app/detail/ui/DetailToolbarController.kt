package com.absinthe.libchecker.domain.app.detail.ui

import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppDetailToolbarAction
import com.absinthe.libchecker.domain.app.detail.model.AppDetailToolbarItem
import com.absinthe.libchecker.domain.app.detail.model.AppDetailToolbarState
import com.absinthe.libchecker.domain.app.detail.ui.adapter.AppDetailToolbarAdapter
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

class DetailToolbarController(
  private val toolbarView: RecyclerView,
  private val appBarLayout: AppBarLayout,
  private val onSortClick: () -> Unit,
  private val onOnlineRuleAnalysisClick: () -> Unit,
  private val onQuickLaunchClick: () -> Unit,
  private val onProcessClick: () -> Unit
) {

  private val adapter = AppDetailToolbarAdapter(::onActionClick)
  private var state = defaultState()
  private var onHarmonyToggle: (() -> Unit)? = null
  private var onCompareClick: (() -> Unit)? = null
  private val offsetChangedListener = AppBarLayout.OnOffsetChangedListener { layout, verticalOffset ->
    updateState {
      it.copy(toolbarCollapsed = abs(verticalOffset) - layout.totalScrollRange == 0)
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
    updateState {
      it.copy(
        baseActionsReady = true,
        harmonyToggleVisible = showHarmonyToggle
      )
    }
  }

  fun addCompareAction(onCompareClick: () -> Unit) {
    this.onCompareClick = onCompareClick
    updateState { it.copy(compareVisible = true) }
  }

  fun setProcessActionVisible(visible: Boolean) {
    updateState { it.copy(processVisible = visible) }
  }

  fun updateProcessTooltip(@StringRes tooltipTextRes: Int) {
    updateState { it.copy(processLabel = toolbarView.context.getString(tooltipTextRes)) }
  }

  fun reset() {
    onHarmonyToggle = null
    onCompareClick = null
    bindState(defaultState())
  }

  fun release() {
    appBarLayout.removeOnOffsetChangedListener(offsetChangedListener)
  }

  private fun updateState(
    transform: (AppDetailToolbarState) -> AppDetailToolbarState
  ) {
    bindState(transform(state))
  }

  private fun bindState(newState: AppDetailToolbarState) {
    if (newState == state) return
    state = newState
    adapter.setList(state.visibleActions.map(::createItem))
  }

  private fun onActionClick(action: AppDetailToolbarAction) {
    when (action) {
      AppDetailToolbarAction.SORT -> onSortClick()
      AppDetailToolbarAction.ONLINE_RULE_ANALYSIS -> onOnlineRuleAnalysisClick()
      AppDetailToolbarAction.QUICK_LAUNCH -> onQuickLaunchClick()
      AppDetailToolbarAction.PROCESS -> onProcessClick()
      AppDetailToolbarAction.HARMONY_TOGGLE -> onHarmonyToggle?.invoke()
      AppDetailToolbarAction.COMPARE -> onCompareClick?.invoke()
    }
  }

  private fun createItem(action: AppDetailToolbarAction): AppDetailToolbarItem {
    val (iconRes, label) = when (action) {
      AppDetailToolbarAction.SORT -> R.drawable.ic_lib_sort to toolbarView.context.getString(R.string.menu_sort)

      AppDetailToolbarAction.ONLINE_RULE_ANALYSIS ->
        R.drawable.ic_cloud_rules to toolbarView.context.getString(R.string.app_detail_online_rules_action)

      AppDetailToolbarAction.QUICK_LAUNCH -> R.drawable.ic_launch to toolbarView.context.getString(R.string.further_operation)

      AppDetailToolbarAction.PROCESS -> R.drawable.ic_processes to state.processLabel

      AppDetailToolbarAction.HARMONY_TOGGLE -> R.drawable.ic_harmonyos_logo to toolbarView.context.getString(R.string.ability)

      AppDetailToolbarAction.COMPARE -> R.drawable.ic_compare to toolbarView.context.getString(R.string.compare_with_current)
    }
    return AppDetailToolbarItem(
      action = action,
      iconRes = iconRes,
      label = label
    )
  }

  private fun defaultState(): AppDetailToolbarState {
    return AppDetailToolbarState(
      processLabel = toolbarView.context.getString(R.string.menu_process)
    )
  }
}
