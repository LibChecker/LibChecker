package com.absinthe.libchecker.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet

abstract class RoundCornerView(context: Context, attributeSet: AttributeSet? = null) :
  AViewGroup(context, attributeSet) {
  private val backgroundPath = Path()
  private var rect = RectF()
  protected var radius: Int = 0

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    resetPath()
  }

  override fun draw(canvas: Canvas) {
    val save = canvas.save()
    canvas.clipPath(backgroundPath)
    super.draw(canvas)
    canvas.restoreToCount(save)
  }

  override fun dispatchDraw(canvas: Canvas) {
    val save = canvas.save()
    canvas.clipPath(backgroundPath)
    super.dispatchDraw(canvas)
    canvas.restoreToCount(save)
  }

  private fun resetPath() {
    backgroundPath.reset()
    backgroundPath.addRoundRect(rect, radius.toFloat(), radius.toFloat(), Path.Direction.CW)
    backgroundPath.close()
  }
}
