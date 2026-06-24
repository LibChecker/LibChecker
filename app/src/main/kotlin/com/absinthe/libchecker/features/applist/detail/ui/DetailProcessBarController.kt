package com.absinthe.libchecker.features.applist.detail.ui

import android.view.ViewGroup
import androidx.core.view.isGone
import com.absinthe.libchecker.features.applist.detail.ui.adapter.ProcessBarAdapter
import com.absinthe.libchecker.features.applist.detail.ui.view.ProcessBarView

class DetailProcessBarController(
  private val container: ViewGroup,
  private val processMode: () -> Boolean,
  private val hasNonGrantedPermissions: () -> Boolean?,
  private val onProcessFilterChanged: (String?) -> Unit
) {
  private var processBarView: ProcessBarView? = null

  fun setData(processMap: Map<String, Int>) {
    if (processMap.isEmpty()) {
      removeProcessBar()
      return
    }

    ensureProcessBar()
    refreshVisibility()
    processBarView?.setData(
      processMap.map { mapItem ->
        ProcessBarAdapter.ProcessBarItem(
          mapItem.key,
          mapItem.value
        )
      }
    )
  }

  fun refreshVisibility() {
    processBarView?.isGone =
      !processMode() &&
      hasNonGrantedPermissions() == false
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
      it.setOnItemClickListener { isSelected, process ->
        onProcessFilterChanged(if (isSelected) process else null)
      }
    }
    container.addView(processBarView)
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
