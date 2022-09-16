package com.absinthe.libchecker.view.detail

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr

class SignatureDetailItemView(context: Context) : LinearLayout(context) {

  val type = AppCompatTextView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
        it.gravity = Gravity.LEFT
      }
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
  }

  val content = AppCompatTextView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
        it.topMargin = 4.dp
        it.gravity = Gravity.LEFT
      }
  }

  init {
    orientation = VERTICAL
    setPadding(8.dp, 2.dp, 8.dp, 2.dp)
    addView(type)
    addView(content)
  }
}
