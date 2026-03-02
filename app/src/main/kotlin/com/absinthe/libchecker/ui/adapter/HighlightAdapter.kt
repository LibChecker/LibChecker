package com.absinthe.libchecker.ui.adapter

import android.widget.TextView
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

abstract class HighlightAdapter<T> : BaseQuickAdapter<T, BaseViewHolder>(0) {

  var highlightText: String = ""

  protected fun setOrHighlightText(view: TextView, text: CharSequence) {
    if (highlightText.isNotBlank()) {
      view.tintHighlightText(highlightText, text)
    } else {
      view.text = text
    }
  }
}
