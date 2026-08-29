package com.absinthe.libchecker.ui.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.addSpacingDecoration(
  space: Int,
  @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
  influenceParent: Boolean = false,
  ratio: Float = 0.5f
) {
  addItemDecoration(
    object : RecyclerView.ItemDecoration() {
      override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
      ) {
        if (orientation == RecyclerView.HORIZONTAL) {
          if (influenceParent && parent.paddingLeft != space) {
            parent.setPadding(space, parent.paddingTop, space, parent.paddingBottom)
            parent.clipToPadding = false
          }
          outRect.set(space, (space * ratio).toInt(), space, (space * ratio).toInt())
        } else {
          if (influenceParent && parent.paddingTop != space) {
            parent.setPadding(parent.paddingStart, space, parent.paddingEnd, space)
            parent.clipToPadding = false
          }
          outRect.set((space * ratio).toInt(), space, (space * ratio).toInt(), space)
        }
      }
    }
  )
}
