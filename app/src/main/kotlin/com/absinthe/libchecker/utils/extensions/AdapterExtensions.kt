package com.absinthe.libchecker.utils.extensions

import android.view.ViewGroup
import android.widget.Space
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

fun BaseQuickAdapter<*, BaseViewHolder>.setSpaceFooterView() {
  recyclerViewOrNull?.let { rv ->
    hasFooterLayout()
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
