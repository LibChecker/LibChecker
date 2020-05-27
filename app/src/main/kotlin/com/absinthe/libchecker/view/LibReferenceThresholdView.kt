package com.absinthe.libchecker.view

import android.content.Context
import com.absinthe.libchecker.constant.GlobalValues
import com.google.android.material.slider.Slider
import com.squareup.contour.ContourLayout

class LibReferenceThresholdView(context: Context) : ContourLayout(context) {

    val slider: Slider = Slider(context).apply {
        valueFrom = 1f
        valueTo = 20f
        stepSize = 1f
        value = GlobalValues.libReferenceThreshold.value?.toFloat() ?: 1f
        applyLayout(
            x = matchParentX(),
            y = topTo { parent.top() }
        )
    }

    init {
        contourHeightOf { slider.height() + 10.ydip }
    }
}