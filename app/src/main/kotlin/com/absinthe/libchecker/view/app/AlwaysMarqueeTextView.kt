package com.absinthe.libchecker.view.app

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class AlwaysMarqueeTextView : AppCompatTextView {

  constructor(context: Context) : super(context) {
    init()
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    init()
  }

  constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  ) {
    init()
  }

  override fun isFocused(): Boolean {
    return true
  }

  private fun init() {
    ellipsize = TextUtils.TruncateAt.MARQUEE
    isFocusable = true
    isFocusableInTouchMode = true
    // Infinity
    marqueeRepeatLimit = -1
    setSingleLine()
  }
}
