package com.absinthe.libchecker.domain.app.detail.ui

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.model.ProcessBarAction
import com.absinthe.libchecker.domain.app.detail.model.ProcessBarRenderState
import com.absinthe.libchecker.domain.app.detail.ui.view.ProcessBarView

class DetailProcessBarController(
  private val container: ViewGroup,
  private val processMode: () -> Boolean,
  private val hasNonGrantedPermissions: () -> Boolean?,
  private val onProcessFilterChanged: (String?) -> Unit
) {
  private var processBarView: ProcessBarView? = null
  private var processColors: Map<String, Int> = emptyMap()
  private var selectedProcess: String? = null
  private var renderState = ProcessBarRenderState()

  fun setData(processMap: Map<String, Int>) {
    processColors = processMap
    if (processMap.isEmpty()) {
      selectedProcess = null
    }
    render()
  }

  fun refreshVisibility() {
    render()
  }

  fun clearSelection() {
    if (selectedProcess == null) {
      return
    }
    selectedProcess = null
    render()
  }

  private fun ensureProcessBar() {
    if (processBarView != null) {
      return
    }

    processBarView = ProcessBarView(container.context).also {
      it.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }
    container.addView(processBarView)
  }

  private fun render() {
    val state = ProcessBarRenderState.create(
      processColors = processColors,
      selectedProcess = selectedProcess,
      processMode = processMode(),
      hasNonGrantedPermissions = hasNonGrantedPermissions()
    )
    if (state == renderState) {
      return
    }
    renderState = state
    if (state.items.isEmpty()) {
      removeProcessBar()
      return
    }
    ensureProcessBar()
    processBarView?.bind(state, ::onAction)
  }

  private fun onAction(action: ProcessBarAction) {
    when (action) {
      is ProcessBarAction.ProcessSelectionChanged -> {
        selectedProcess = action.process
        render()
        onProcessFilterChanged(action.process)
      }
    }
  }

  private fun removeProcessBar() {
    processBarView?.let {
      if (it.parent != null) {
        (it.parent as? ViewGroup)?.removeView(it)
      }
    }
    processBarView = null
  }
}
