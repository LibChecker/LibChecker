package com.absinthe.libchecker.ui.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * <pre>
 * author : Absinthe
 * time : 2021/03/23
 * </pre>
 */
class VerticalSpacesItemDecoration(
  private val space: Int,
  private val influenceParent: Boolean = false,
  private val ratio: Float = 0.5f
) : RecyclerView.ItemDecoration() {

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    if (influenceParent && parent.paddingTop != space) {
      parent.setPadding(parent.paddingStart, space, parent.paddingEnd, space)
      parent.clipToPadding = false
    }

    outRect.apply {
      left = (space * ratio).toInt()
      right = (space * ratio).toInt()
      bottom = space
      top = space
    }
  }
}
