package com.absinthe.libchecker.utils.extensions

import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.HeaderFooterSupport

fun HeaderFooterSupport.setSpaceFooterView() {
  val hasEmptyState = this is BaseQuickAdapter<*, *> && isStateViewEnable && items.isEmpty()
  recyclerViewOrNull?.setBottomPaddingSpace(hasEmptyState)
}

fun RecyclerView.setBottomPaddingSpace(hasEmptyState: Boolean = false) {
  val addedPadding = getTag(R.id.adapter_bottom_padding_id) == true
  fun should(): Boolean {
    // Empty rows need their final viewport before any children have been laid out.
    if (hasEmptyState) return true
    val a = childCount
    val b = adapter?.itemCount ?: 0
    return if (!addedPadding) {
      a >= b
    } else {
      a >= b - 1
    }
  }
  if (should()) {
    if (addedPadding) return
    setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + 96.dp)
    setTag(R.id.adapter_bottom_padding_id, true)
  } else {
    if (!addedPadding) return
    setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom - 96.dp)
    setTag(R.id.adapter_bottom_padding_id, false)
  }
}
