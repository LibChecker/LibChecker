package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppBundleItem
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.TextColumnRowView

class AppBundleItemView(context: Context) : TextColumnRowView(context) {

  private val icon = AppCompatImageView(context).apply {
    layoutParams = ViewGroup.LayoutParams(24.dp, 24.dp)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  private val name = addTextLine(R.style.TextView_SansSerifMedium) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }
  private val size = addTextLine(R.style.TextView_SansSerifCondensedMedium) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
  }

  init {
    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
    setLeadingView(icon)
  }

  fun bind(item: AppBundleItem) {
    icon.setImageResource(item.iconRes)
    name.text = item.nameText
    size.text = item.sizeText
    contentDescription = item.contentDescription
  }
}
