package com.absinthe.libchecker.features.chart.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libraries.utils.extensions.paddingEndCompat

class ExpandingView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

  private val icon: ImageView
  private val text: TextView
  private val collapsedSize = 48.dp
  private var expandedWidth = collapsedSize
  private var isExpanded = false

  init {
    layoutParams = LayoutParams(collapsedSize, collapsedSize)
    orientation = HORIZONTAL
    setBackgroundResource(R.drawable.ripple_feature_label_48dp)
    icon = ImageView(context).apply {
      layoutParams = LayoutParams(collapsedSize, collapsedSize).also {
        it.gravity = Gravity.CENTER_VERTICAL
      }
      scaleType = ImageView.ScaleType.CENTER_INSIDE
    }
    text = TextView(context).apply {
      layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
        it.gravity = Gravity.CENTER_VERTICAL
      }
      paddingEndCompat = 16.dp
      isSingleLine = true
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
      setTypeface(null, Typeface.BOLD)
      visibility = GONE
    }

    addView(icon)
    addView(text)
  }

  fun setContent(iconRes: Int, content: String) {
    icon.setImageResource(iconRes)
    text.text = content
  }

  private fun measureExpandedWidth() {
    measureChild(icon, MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
    measureChild(text, MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
    expandedWidth = icon.measuredWidth + text.measuredWidth + paddingLeft + paddingRight
  }

  fun toggle() {
    measureExpandedWidth()

    val animator = if (isExpanded) {
      ValueAnimator.ofInt(expandedWidth, collapsedSize)
    } else {
      ValueAnimator.ofInt(collapsedSize, expandedWidth)
    }

    animator.addUpdateListener { animation ->
      val value = animation.animatedValue as Int
      expandedWidth = value
      layoutParams.width = value
      text.isVisible = value > collapsedSize

      requestLayout()
    }

    animator.interpolator = AccelerateDecelerateInterpolator()
    animator.duration = 300
    animator.start()

    isExpanded = !isExpanded
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    setMeasuredDimension(expandedWidth, collapsedSize)
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)

    val iconLeft = paddingLeft
    val iconTop = (height - icon.measuredHeight) / 2
    val iconRight = iconLeft + icon.measuredWidth
    val iconBottom = iconTop + icon.measuredHeight
    icon.layout(iconLeft, iconTop, iconRight, iconBottom)

    val textLeft = iconRight + 2.dp
    val textTop = (height - text.measuredHeight) / 2
    val textRight = textLeft + text.measuredWidth
    val textBottom = textTop + text.measuredHeight
    text.layout(textLeft, textTop, textRight, textBottom)
  }
}
