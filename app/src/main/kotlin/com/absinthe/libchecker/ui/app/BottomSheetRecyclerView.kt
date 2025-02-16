package com.absinthe.libchecker.ui.app

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
        if (deltaY > 0 && canScrollVertically(-1)) {
          parent.requestDisallowInterceptTouchEvent(true)
        }
      }

      MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
        parent.requestDisallowInterceptTouchEvent(false)
      }
    }

    return super.dispatchTouchEvent(ev)
  }
}
