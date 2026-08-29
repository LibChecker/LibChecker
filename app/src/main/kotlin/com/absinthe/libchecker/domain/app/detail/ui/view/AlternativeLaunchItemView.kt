package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AlternativeLaunchItem
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.TextColumnRowView

class AlternativeLaunchItemView(context: Context) : TextColumnRowView(context) {

  private val label = addTextLine(R.style.TextView_SansSerifMedium) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }
  private val className = addTextLine(R.style.TextView_SansSerifCondensedMedium) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
  }

  init {
    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
  }

  fun bind(item: AlternativeLaunchItem) {
    label.text = item.label
    className.text = item.className
    contentDescription = item.contentDescription
  }
}
