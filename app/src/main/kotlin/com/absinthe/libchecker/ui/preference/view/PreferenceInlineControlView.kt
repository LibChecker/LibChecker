package com.absinthe.libchecker.ui.preference.view

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.preference.model.PreferenceInlineControl
import com.absinthe.libchecker.utils.extensions.dp
import com.google.android.material.slider.Slider

class PreferenceInlineControlView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

  fun bind(
    title: String?,
    control: PreferenceInlineControl,
    onChoiceSelected: (String) -> Unit,
    onRangeValueChangeFinished: (Int) -> Unit
  ) {
    removeAllViews()
    when (control) {
      is PreferenceInlineControl.DraggableChoice -> {
        addView(
          DraggableSegmentedControlView(context, control, onChoiceSelected),
          LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
      }

      is PreferenceInlineControl.IconSegmentedChoice -> {
        addView(
          DraggableSegmentedControlView(context, control, onChoiceSelected),
          LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
      }

      is PreferenceInlineControl.Range -> {
        addView(
          buildRangeControl(title, control, onRangeValueChangeFinished),
          LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
      }
    }
  }

  private fun buildRangeControl(
    title: String?,
    control: PreferenceInlineControl.Range,
    onRangeValueChangeFinished: (Int) -> Unit
  ): LinearLayout {
    var currentValue = control.value
    var committedValue = control.value
    var trackingTouch = false
    fun commitCurrentValue() {
      if (currentValue != committedValue) {
        committedValue = currentValue
        onRangeValueChangeFinished(currentValue)
      }
    }
    val valueView = TextView(context).apply {
      minWidth = 36.dp
      text = currentValue.toString()
      textAlignment = TEXT_ALIGNMENT_VIEW_END
      setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
    }
    val slider = Slider(context).apply {
      layoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
          marginStart = -RANGE_SLIDER_START_ALIGNMENT_DP.dp
        }
      valueFrom = control.valueFrom.toFloat()
      valueTo = control.valueTo.toFloat()
      stepSize = 1f
      value = control.value.toFloat()
      contentDescription = title
      addOnChangeListener { view, value, fromUser ->
        currentValue = value.toInt()
        valueView.text = currentValue.toString()
        if (fromUser) {
          view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
          if (!trackingTouch) {
            commitCurrentValue()
          }
        }
      }
      addOnSliderTouchListener(
        object : Slider.OnSliderTouchListener {
          override fun onStartTrackingTouch(slider: Slider) {
            trackingTouch = true
          }

          override fun onStopTrackingTouch(slider: Slider) {
            trackingTouch = false
            commitCurrentValue()
          }
        }
      )
    }
    return LinearLayout(context).apply {
      gravity = android.view.Gravity.CENTER_VERTICAL
      orientation = LinearLayout.HORIZONTAL
      setPadding(0, 0, RANGE_CONTENT_END_PADDING_DP.dp, 0)
      addView(slider)
      addView(
        valueView,
        LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
          marginStart = 12.dp
        }
      )
    }
  }

  private companion object {
    const val RANGE_SLIDER_START_ALIGNMENT_DP = 19
    const val RANGE_CONTENT_END_PADDING_DP = 8
  }
}
