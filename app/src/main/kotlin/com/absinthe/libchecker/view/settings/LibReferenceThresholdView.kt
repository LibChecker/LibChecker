package com.absinthe.libchecker.view.settings

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.TextView
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.slider.Slider

class LibReferenceThresholdView(context: Context) : AViewGroup(context) {

  val slider: Slider = Slider(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    valueFrom = 1f
    valueTo = 50f
    stepSize = 1f
    value = GlobalValues.libReferenceThreshold.toFloat()
    addView(this)
  }
  val count: TextView = TextView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    setTypeface(null, Typeface.BOLD)
    addView(this)
  }

  init {
    slider.addOnChangeListener { _, value, _ ->
      count.text = value.toInt().toString()
      slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    slider.autoMeasure()
    count.autoMeasure()
    setMeasuredDimension(measuredWidth, slider.measuredHeight + count.measuredHeight)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    slider.layout(0, 0)
    count.let { it.layout(it.toHorizontalCenter(this), slider.measuredHeight) }
  }
}
