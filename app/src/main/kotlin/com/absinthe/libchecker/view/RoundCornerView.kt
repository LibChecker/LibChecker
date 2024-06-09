package com.absinthe.libchecker.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import com.absinthe.libchecker.utils.extensions.getColorByAttr

abstract class RoundCornerView(
  context: Context,
  attributeSet: AttributeSet? = null
) : AViewGroup(context, attributeSet) {
  private val backgroundPath = Path()
  private val paint by lazy {
    Paint().apply {
      style = Paint.Style.STROKE
      strokeWidth = 2.dp.toFloat()
      color = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
    }
  }
  private var rect = RectF()
  protected var radius: Int = 0
  protected var shouldDrawStroke: Boolean = false

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    resetPath()
  }

  override fun draw(canvas: Canvas) {
    val save = canvas.save()
    canvas.clipPath(backgroundPath)
    if (shouldDrawStroke) {
      canvas.drawPath(backgroundPath, paint)
    }
    super.draw(canvas)
    canvas.restoreToCount(save)
  }

  override fun dispatchDraw(canvas: Canvas) {
    val save = canvas.save()
    canvas.clipPath(backgroundPath)
    if (shouldDrawStroke) {
      canvas.drawPath(backgroundPath, paint)
    }
    super.dispatchDraw(canvas)
    canvas.restoreToCount(save)
  }

  private fun resetPath() {
    backgroundPath.reset()
    backgroundPath.addRoundRect(rect, radius.toFloat(), radius.toFloat(), Path.Direction.CW)
    backgroundPath.close()
  }
}
