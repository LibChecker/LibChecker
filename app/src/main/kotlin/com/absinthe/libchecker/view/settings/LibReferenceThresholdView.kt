package com.absinthe.libchecker.view.settings

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.absinthe.libchecker.constant.GlobalValues
import com.google.android.material.slider.Slider

class LibReferenceThresholdView(context: Context) : ConstraintLayout(context) {

    val slider: Slider = Slider(context).apply {
        valueFrom = 1f
        valueTo = 50f
        stepSize = 1f
        value = GlobalValues.libReferenceThreshold.value?.toFloat() ?: 1f
    }
    val count: TextView = TextView(context)

    init {
        id = View.generateViewId()
        slider.id = View.generateViewId()
        count.id = View.generateViewId()

        slider.addOnChangeListener { _, value, _ ->
            count.text = value.toInt().toString()
        }

        addView(slider, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topToTop = this@LibReferenceThresholdView.id
        })
        addView(count, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topToBottom = slider.id
            startToStart = this@LibReferenceThresholdView.id
            endToEnd = this@LibReferenceThresholdView.id
        })
    }
}