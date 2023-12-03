package com.absinthe.libchecker.features.settings.ui

import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.removeNonDigits
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.slider.Slider

class LibReferenceThresholdView(context: Context) : AViewGroup(context) {

  private var initialized = false
  private var sliderChanging = false
  private var countChanging = false

  val slider: Slider = Slider(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    valueFrom = 1f
    valueTo = 50f
    stepSize = 1f
    value = GlobalValues.libReferenceThreshold.toFloat()
    addView(this)
  }
  val count: EditText = EditText(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    inputType = InputType.TYPE_CLASS_NUMBER
    gravity = Gravity.CENTER
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    setTypeface(null, Typeface.BOLD)
    addView(this)
  }

  init {
    slider.addOnChangeListener { _, value, _ ->
      if (!countChanging) {
        sliderChanging = true
        count.apply {
          setText(value.toInt().toString())
          if (UiUtils.isSoftInputOpen()) {
            text?.let { setSelection(it.length) }
          }
        }
        slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
      } else {
        countChanging = false
      }
    }
    count.doOnTextChanged { text, _, _, _ ->
      if (!initialized) {
        initialized = true
        return@doOnTextChanged
      }
      if (!sliderChanging) {
        countChanging = true
        text?.toString().let {
          count.apply {
            if (it.isNullOrEmpty()) {
              setText(2f.toInt().toString())
              setSelection(1)
            } else {
              val c = it.removeNonDigits().toFloat()
              if (c < 1f) {
                setText(1f.toInt().toString())
                setSelection(1)
              } else if (c > 50f) {
                setText(50f.toInt().toString())
                setSelection(2)
              } else {
                slider.value = c
              }
            }
          }
        }
      } else {
        sliderChanging = false
      }
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
