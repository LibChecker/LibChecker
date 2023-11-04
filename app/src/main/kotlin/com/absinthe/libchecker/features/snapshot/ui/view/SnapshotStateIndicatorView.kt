package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColor

class SnapshotStateIndicatorView(context: Context) : View(context) {
  var enableRoundCorner: Boolean = true
  var added: Boolean = false
  var removed: Boolean = false
  var changed: Boolean = false
  var moved: Boolean = false

  private val trueValueCount: Int
    get() {
      var count = 0
      if (added) count++
      if (removed) count++
      if (changed) count++
      if (moved) count++
      return count
    }
  private var p: Paint = Paint().apply {
    isAntiAlias = true
  }
  private var eachItemHeight: Float = 0f
  private var drawOverPosition: Float = 0f

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (enableRoundCorner) {
      outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
          outline?.setRoundRect(0, 0, measuredWidth, measuredHeight, (measuredWidth / 2).toFloat())
        }
      }
      clipToOutline = true
    }
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (trueValueCount == 0) {
      return
    }
    eachItemHeight = (measuredHeight / trueValueCount).toFloat()
    drawOverPosition = 0f

    if (added) {
      drawItem(R.color.material_green_300, canvas)
    }
    if (removed) {
      drawItem(R.color.material_red_300, canvas)
    }
    if (changed) {
      drawItem(R.color.material_yellow_300, canvas)
    }
    if (moved) {
      drawItem(R.color.material_blue_300, canvas)
    }
  }

  private fun drawItem(@ColorRes color: Int, canvas: Canvas) {
    p.color = color.getColor(context)
    canvas.drawRect(
      0f,
      drawOverPosition,
      measuredWidth.toFloat(),
      drawOverPosition + eachItemHeight,
      p
    )
    drawOverPosition += eachItemHeight
  }
}
