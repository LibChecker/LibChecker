package com.absinthe.libchecker.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import rikka.widget.borderview.BorderRecyclerView

class BottomSheetRecyclerView : BorderRecyclerView {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  )

  private var lastY = 0f

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    when (ev.action) {
      MotionEvent.ACTION_DOWN -> {
        lastY = ev.y
      }
      MotionEvent.ACTION_MOVE -> {
        val deltaY = ev.y - lastY
        parent.requestDisallowInterceptTouchEvent(deltaY > 0 && canScrollVertically(-1))
      }
    }

    return super.dispatchTouchEvent(ev)
  }
}
