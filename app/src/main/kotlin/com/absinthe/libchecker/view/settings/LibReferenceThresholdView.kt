package com.absinthe.libchecker.view.settings

import android.content.Context
import android.view.View
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

    init {
        id = View.generateViewId()

        addView(slider, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topToTop = this@LibReferenceThresholdView.id
        })
    }
}