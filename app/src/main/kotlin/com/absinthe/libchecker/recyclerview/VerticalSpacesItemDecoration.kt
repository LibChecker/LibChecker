package com.absinthe.libchecker.recyclerview

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
  private val influenceParent: Boolean = false
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
      left = space / 2
      right = space / 2
      bottom = space
      top = space
    }
  }
}
