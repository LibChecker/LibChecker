package com.absinthe.libchecker.view.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import android.view.RoundedCorner
import android.view.WindowInsets
import androidx.core.graphics.ColorUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.drawable.G2CornerTreatment
import com.google.android.material.shape.AbsoluteCornerSize
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapePath

class BottomSheetBackgroundDrawable(context: Context, source: MaterialShapeDrawable) : MaterialShapeDrawable(source.shapeAppearanceModel) {

  private val cornerTreatment = G2CornerTreatment()
  private val defaultShape = source.shapeAppearanceModel.toBuilder()
    .setTopLeftCorner(cornerTreatment)
    .setTopRightCorner(cornerTreatment)
    .build()
  private val topEdge = Path()
  private val cornerPath = ShapePath()
  private val cornerTransform = Matrix()
  private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = context.resources.displayMetrics.density
  }

  init {
    shapeAppearanceModel = defaultShape
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

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    if (bounds.width() <= 0) return
    val color = ColorUtils.setAlphaComponent(edgePaint.color, 255)
    val transparent = ColorUtils.setAlphaComponent(color, 0)
    edgePaint.shader = LinearGradient(
      bounds.left.toFloat(),
      0f,
      bounds.right.toFloat(),
      0f,
      intArrayOf(transparent, color, color, transparent),
      floatArrayOf(0f, 0.12f, 0.88f, 1f),
      Shader.TileMode.CLAMP
    )
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
    cornerTreatment.getCornerPath(cornerPath, 90f, 1f, leftArc)
    topEdge.moveTo(left, top + cornerPath.startY)
    cornerTransform.setTranslate(left, top)
    cornerPath.applyToPath(cornerTransform, topEdge)
    cornerTreatment.getCornerPath(cornerPath, 90f, 1f, rightArc)
    topEdge.lineTo(right - cornerPath.startY, top)
    cornerTransform.setRotate(90f)
    cornerTransform.postTranslate(right, top)
    cornerPath.applyToPath(cornerTransform, topEdge)
    edgePaint.alpha = alpha
    canvas.drawPath(topEdge, edgePaint)
  }
}
