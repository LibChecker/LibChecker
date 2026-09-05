package com.absinthe.libchecker.view.app

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.roundToInt

class FloatingBottomNavigationView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs),
  FloatingNavigationBar {
  private val thumbController = FloatingNavigationThumbController(
    host = this,
    vertical = false
  )

  override var isFloating: Boolean
    get() = thumbController.isFloating
    set(value) {
      thumbController.isFloating = value
    }

  override val currentFloatingProgress: Float
    get() = thumbController.currentFloatingProgress

  override fun setFloatingProgress(progress: Float) = thumbController.setFloatingProgress(progress)

  override fun setBlurProgress(progress: Float) = thumbController.setBlurProgress(progress)

  override fun suppressDragUntilRelease() = thumbController.suppressDragUntilRelease()

  override fun setSelectedIndex(index: Int, animate: Boolean) = thumbController.setSelectedIndex(index, animate)

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    thumbController.onDispatchTouchEvent(event)
    return super.dispatchTouchEvent(event)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (currentFloatingProgress > 0f) {
      val floatingHeight = 56 * resources.displayMetrics.density + paddingTop + paddingBottom
      val height = (measuredHeight + (floatingHeight - measuredHeight) * currentFloatingProgress).roundToInt()
      val resolvedHeightSpec = MeasureSpec.makeMeasureSpec(
        resolveSize(height, heightMeasureSpec),
        MeasureSpec.EXACTLY
      )
      super.onMeasure(widthMeasureSpec, resolvedHeightSpec)
    }
    thumbController.onMeasure()
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    thumbController.onLayout()
  }

  override fun onInterceptTouchEvent(event: MotionEvent): Boolean = thumbController.onInterceptTouchEvent(event) ?: super.onInterceptTouchEvent(event)

  override fun onTouchEvent(event: MotionEvent): Boolean = thumbController.onTouchEvent(event) ?: super.onTouchEvent(event)

  override fun onDetachedFromWindow() {
    thumbController.onDetachedFromWindow()
    super.onDetachedFromWindow()
  }
}
