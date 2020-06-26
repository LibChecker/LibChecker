package com.absinthe.libchecker.view.detail

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.google.android.material.chip.Chip
import com.squareup.contour.ContourLayout

class LibStringItemView(context: Context) : ContourLayout(context) {

    val tvName: TextView = TextView(context).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        setTextColor(ContextCompat.getColor(context, R.color.textNormal))
        LayoutSpec(
            x = leftTo { parent.left() },
            y = topTo { parent.top() }
        )
    }

    val tvLibSize: TextView = TextView(context).apply {
        typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        LayoutSpec(
            x = leftTo { parent.left() },
            y = topTo { tvName.bottom() }
        )
    }

    val chip: Chip = Chip(ContextThemeWrapper(context, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Action)).apply {
        chipMinHeight = 28.dip.toFloat()
        chipStartPadding = 10.dip.toFloat()
        LayoutSpec(
            x = rightTo { parent.right() },
            y = centerVerticallyTo { parent.height() }
        )
    }

    init {
        minimumHeight = 40
    }
}