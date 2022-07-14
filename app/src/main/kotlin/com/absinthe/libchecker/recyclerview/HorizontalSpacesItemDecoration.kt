package com.absinthe.libchecker.recyclerview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/19
 * </pre>
 */
class HorizontalSpacesItemDecoration(
  private val space: Int,
  private val influenceParent: Boolean = false
) : RecyclerView.ItemDecoration() {

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    if (influenceParent && parent.paddingLeft != space) {
      parent.setPadding(space, parent.paddingTop, space, parent.paddingBottom)
      parent.clipToPadding = false
    }

    outRect.apply {
      left = space
      right = space
      bottom = space / 2
      top = space / 2
    }
  }
}
