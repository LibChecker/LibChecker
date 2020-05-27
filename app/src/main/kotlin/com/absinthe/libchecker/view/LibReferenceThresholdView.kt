package com.absinthe.libchecker.view

import android.content.Context
import com.google.android.material.slider.Slider
import com.squareup.contour.ContourLayout

class LibReferenceThresholdView(context: Context) : ContourLayout(context) {

    val slider: Slider = Slider(context).apply {
        valueFrom = 1f
        valueTo = 50f
        stepSize = 1f
        applyLayout(
            x = centerHorizontallyTo { parent.width() },
            y = topTo { parent.top() }
        )
    }

}