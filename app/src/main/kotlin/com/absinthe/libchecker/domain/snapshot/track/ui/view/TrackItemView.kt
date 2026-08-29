package com.absinthe.libchecker.domain.snapshot.track.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.app.TextColumnRowView
import com.google.android.material.checkbox.MaterialCheckBox

class TrackItemView(context: Context) : TextColumnRowView(context) {

  val container: TrackItemView
    get() = this

  val icon = AppCompatImageView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.app_icon_size)
    layoutParams = ViewGroup.LayoutParams(size, size)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  val appName = addTextLine(R.style.TextView_SansSerifMedium) {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
  }
  val packageName = addTextLine(R.style.TextView_SansSerif) {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }
  val switch = MaterialCheckBox(context).apply {
    id = android.R.id.toggle
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    background = null
  }

  init {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackground))
    setLeadingView(icon)
    setTrailingView(switch, spacing = 0)
  }
}
