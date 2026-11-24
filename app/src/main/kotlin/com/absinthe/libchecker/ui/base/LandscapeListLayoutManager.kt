package com.absinthe.libchecker.ui.base

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

internal class LandscapeListLayoutManager : StaggeredGridLayoutManager(2, VERTICAL) {
  private var laidOutSinceAttach = false

  override fun onAttachedToWindow(view: RecyclerView) {
    super.onAttachedToWindow(view)
    laidOutSinceAttach = false
  }

  override fun onLayoutCompleted(state: RecyclerView.State) {
    super.onLayoutCompleted(state)
    laidOutSinceAttach = true
  }

  override fun onScrollStateChanged(state: Int) {
    // Detach clears the spans but retains child views until the next layout.
    if (state == RecyclerView.SCROLL_STATE_IDLE && !laidOutSinceAttach) return
    super.onScrollStateChanged(state)
  }
}
