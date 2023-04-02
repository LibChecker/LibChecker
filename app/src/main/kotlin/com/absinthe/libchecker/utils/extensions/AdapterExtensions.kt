package com.absinthe.libchecker.utils.extensions

import android.view.ViewGroup
import android.widget.Space
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

fun BaseQuickAdapter<*, BaseViewHolder>.setSpaceFooterView() {
  recyclerViewOrNull?.let { rv ->
    fun should(): Boolean {
      val a = rv.childCount
      val b = itemCount
      return if (!hasFooterLayout()) {
        a >= b
      } else {
        a >= b - 1
      }
    }
    if (should()) {
      if (hasFooterLayout()) return
      Space(rv.context).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          96.dp
        )
      }.also {
        setFooterView(it)
      }
    } else {
      if (!hasFooterLayout()) return
      removeAllFooterView()
    }
  }
}

fun RecyclerView.setBottomPaddingSpace() {
  val addedPadding = getTag(R.id.adapter_bottom_padding_id)?.toString().orEmpty().isNotBlank()
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
