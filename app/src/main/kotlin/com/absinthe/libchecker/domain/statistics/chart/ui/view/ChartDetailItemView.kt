package com.absinthe.libchecker.domain.statistics.chart.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.app.TextColumnRowView

class ChartDetailItemView(context: Context) : TextColumnRowView(context) {

  val container: ChartDetailItemView
    get() = this

  val icon = AppCompatImageButton(context).apply {
    id = android.R.id.icon
    val size = context.getDimensionPixelSize(R.dimen.app_icon_size)
    layoutParams = ViewGroup.LayoutParams(size, size)
    scaleType = ImageView.ScaleType.CENTER_INSIDE
    setBackgroundResource(R.drawable.bg_circle_secondary_container)
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  val labelName = addTextLine(R.style.TextView_SansSerifMedium) {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }
  val count = createTextView(R.style.TextView_SansSerif) {
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadlineMedium))
  }

  init {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackground))
    setLeadingView(icon)
    setTrailingView(count)
    centerTextColumn()
  }
}
