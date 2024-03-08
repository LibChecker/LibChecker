package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.absoluteValue
import kotlin.math.pow
import rikka.widget.borderview.BorderRecyclerView

class ComponentRecyclerView : BorderRecyclerView {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  )

  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private var lastX = 0f
  private var lastY = 0f

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    when (ev.action) {
      MotionEvent.ACTION_DOWN -> {
        parent.requestDisallowInterceptTouchEvent(true)
        lastX = ev.x
        lastY = ev.y
      }

      MotionEvent.ACTION_MOVE -> {
        val deltaX = (ev.x - lastX).absoluteValue
        val deltaY = (ev.y - lastY).absoluteValue

        // Intercept parent touch event if user swipe horizontally
        if (
          (deltaX.pow(2) + deltaY.pow(2) > touchSlop.toFloat().pow(2)) &&
          (deltaX > deltaY * 1.5)
        ) {
          parent.requestDisallowInterceptTouchEvent(false)
        }
      }
    }

    return super.dispatchTouchEvent(ev)
  }
}
