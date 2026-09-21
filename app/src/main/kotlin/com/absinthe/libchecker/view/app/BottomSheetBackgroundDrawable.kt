package com.absinthe.libchecker.view.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.RoundedCorner
import android.view.WindowInsets
import androidx.core.graphics.ColorUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.google.android.material.shape.AbsoluteCornerSize
import com.google.android.material.shape.MaterialShapeDrawable

class BottomSheetBackgroundDrawable(context: Context, source: MaterialShapeDrawable) : MaterialShapeDrawable(source.shapeAppearanceModel) {

  private val defaultShape = source.shapeAppearanceModel
  private val topEdge = Path()
  private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = context.resources.displayMetrics.density
  }

  init {
    initializeElevationOverlay(context)
    fillColor = source.fillColor
    tintList = source.tintList
    elevation = source.elevation
    val backgroundColor = tintList?.defaultColor ?: fillColor?.defaultColor
      ?: context.getColorByAttr(com.google.android.material.R.attr.colorSurfaceContainerLow)
    edgePaint.color = ColorUtils.blendARGB(
      compositeElevationOverlayIfNeeded(backgroundColor),
      context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface),
      0.04f
    )
  }

  fun updateCorners(insets: WindowInsets?) {
    if (!OsUtils.atLeastS()) return
    val left = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius
    val right = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius
    shapeAppearanceModel = defaultShape.toBuilder()
      .setTopLeftCornerSize(left?.let { AbsoluteCornerSize(it.toFloat()) } ?: defaultShape.topLeftCornerSize)
      .setTopRightCornerSize(right?.let { AbsoluteCornerSize(it.toFloat()) } ?: defaultShape.topRightCornerSize)
      .build()
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)
    val inset = edgePaint.strokeWidth / 2f
    val leftRadius = topLeftCornerResolvedSize
    val rightRadius = topRightCornerResolvedSize
    val left = bounds.left + inset
    val top = bounds.top + inset
    val right = bounds.right - inset
    val leftArc = (leftRadius - inset).coerceAtLeast(0f)
    val rightArc = (rightRadius - inset).coerceAtLeast(0f)
    topEdge.rewind()
    topEdge.moveTo(left, top + leftArc)
    if (leftArc > 0f) {
      topEdge.arcTo(left, top, left + 2 * leftArc, top + 2 * leftArc, 180f, 90f, false)
    }
    topEdge.lineTo(right - rightArc, top)
    if (rightArc > 0f) {
      topEdge.arcTo(right - 2 * rightArc, top, right, top + 2 * rightArc, 270f, 90f, false)
    }
    edgePaint.alpha = alpha
    canvas.drawPath(topEdge, edgePaint)
  }
}
