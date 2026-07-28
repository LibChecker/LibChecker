package com.absinthe.libchecker.ui.app

import android.content.Context
import android.util.AttributeSet
import rikka.widget.borderview.BorderRecyclerView

class BottomSheetRecyclerView : BorderRecyclerView {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  )

  init {
    // BottomSheetBehavior discovers and coordinates its scrolling child through nested scrolling.
    isNestedScrollingEnabled = true
  }
}
