package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr

class SystemPropItemView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

  val tvTitle =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
      layoutParams = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT
      )
    }

  val tvText =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifBlack)).apply {
      layoutParams = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT
      )
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    }

  init {
    orientation = VERTICAL
    setPadding(0, 3.dp, 0, 3.dp)
    addView(tvTitle)
    addView(tvText)
  }
}
