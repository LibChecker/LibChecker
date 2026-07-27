package com.absinthe.libchecker.view.app

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class AlwaysMarqueeTextView : AppCompatTextView {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  )

  override fun isFocused(): Boolean {
    return true
  }

  init {
    ellipsize = TextUtils.TruncateAt.MARQUEE
    isFocusable = true
    isFocusableInTouchMode = true
    // Infinity
    marqueeRepeatLimit = -1
    setSingleLine()
  }
}
