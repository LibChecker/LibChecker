package com.absinthe.libchecker.utils.extensions

import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.chad.library.adapter.base.HeaderFooterSupport

fun HeaderFooterSupport.setSpaceFooterView() {
  recyclerViewOrNull?.setBottomPaddingSpace()
}

fun RecyclerView.setBottomPaddingSpace() {
  val addedPadding = getTag(R.id.adapter_bottom_padding_id) == true
  fun should(): Boolean {
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
