package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.ColorUtils
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView
import kotlin.math.min

@RequiresApi(Build.VERSION_CODES.O)
class AdaptiveIconLayerCardView(
  context: Context,
  icon: AdaptiveIconDrawable,
  onBackgroundClick: () -> Unit,
  onForegroundClick: () -> Unit
) : MaterialCardView(context) {

  private val stitchInset = STITCH_INSET_DP.dp.toFloat()
  private val stitchRadius = STITCH_RADIUS_DP.dp.toFloat()
  private val stitchBounds = RectF()
  private val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    strokeWidth = STITCH_STROKE_WIDTH_DP.dp.toFloat()
    color = ColorUtils.setAlphaComponent(
      context.getColorByAttr(com.google.android.material.R.attr.colorOutline),
      STITCH_ALPHA
    )
    pathEffect = DashPathEffect(
      floatArrayOf(
        STITCH_DASH_LENGTH_DP.dp.toFloat(),
        STITCH_DASH_GAP_DP.dp.toFloat()
      ),
      0f
    )
  }
  private val contentView = ContentView(
    context = context,
    icon = icon,
    onBackgroundClick = onBackgroundClick,
    onForegroundClick = onForegroundClick
  )

  init {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setSmoothRoundCorner(CARD_RADIUS_DP.dp)
    cardElevation = CARD_ELEVATION_DP.dp.toFloat()
    setCardBackgroundColor(
      context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh)
    )
    isClickable = true
    isFocusable = false
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    setOnClickListener { }
    addView(contentView)
  }

  fun fitPreviewSize(desiredSize: Int, maxWidth: Int) {
    contentView.fitPreviewSize(desiredSize, maxWidth)
  }

  internal val previewCenterOffset: Int
    get() = contentView.top + contentView.previewCenterOffset

  override fun dispatchDraw(canvas: Canvas) {
    super.dispatchDraw(canvas)
    val inset = stitchInset + stitchPaint.strokeWidth / 2f
    stitchBounds.set(inset, inset, width - inset, height - inset)
    canvas.drawRoundRect(stitchBounds, stitchRadius, stitchRadius, stitchPaint)
  }

  private class ContentView(
    context: Context,
    icon: AdaptiveIconDrawable,
    onBackgroundClick: () -> Unit,
    onForegroundClick: () -> Unit
  ) : AViewGroup(context) {

    private val contentPadding = CONTENT_PADDING_DP.dp
    private val backgroundToPlusGap = BACKGROUND_TO_PLUS_GAP_DP.dp
    private val plusToForegroundGap = PLUS_TO_FOREGROUND_GAP_DP.dp
    private val plusSize = PLUS_SIZE_DP.dp
    private var previewSize = DEFAULT_PREVIEW_SIZE_DP.dp

    val previewCenterOffset: Int
      get() = paddingTop + previewSize / 2

    private val backgroundView = LayerPreviewView(
      context = context,
      drawable = createMaskedBackgroundDrawable(icon),
      label = context.getString(R.string.adaptive_icon_layer_background),
      actionDescription = context.getString(R.string.adaptive_icon_copy_background),
      onClick = onBackgroundClick
    )
    private val plusView = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(plusSize, plusSize)
      scaleType = ImageView.ScaleType.CENTER_INSIDE
      setImageResource(R.drawable.ic_add)
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
      isClickable = false
      isFocusable = false
    }
    private val foregroundView = LayerPreviewView(
      context = context,
      drawable = icon.foreground.copyDrawable(),
      label = context.getString(R.string.adaptive_icon_layer_foreground),
      actionDescription = context.getString(R.string.adaptive_icon_copy_foreground),
      onClick = onForegroundClick
    )

    init {
      setPadding(contentPadding, contentPadding, contentPadding, contentPadding)
      addView(backgroundView)
      addView(plusView)
      addView(foregroundView)
      updatePreviewSize(previewSize)
    }

    fun fitPreviewSize(desiredSize: Int, maxWidth: Int) {
      val fixedWidth =
        paddingStart + paddingEnd + plusSize + backgroundToPlusGap + plusToForegroundGap
      val availablePreviewWidth = ((maxWidth - fixedWidth) / 2).coerceAtLeast(MIN_PREVIEW_SIZE_DP.dp)
      updatePreviewSize(min(desiredSize, availablePreviewWidth))
    }

    private fun updatePreviewSize(size: Int) {
      previewSize = size
      backgroundView.setPreviewSize(size)
      foregroundView.setPreviewSize(size)
      requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      backgroundView.autoMeasure()
      plusView.autoMeasure()
      foregroundView.autoMeasure()
      setMeasuredDimension(
        paddingStart +
          backgroundView.measuredWidth +
          backgroundToPlusGap +
          plusView.measuredWidth +
          plusToForegroundGap +
          foregroundView.measuredWidth +
          paddingEnd,
        paddingTop +
          maxOf(backgroundView.measuredHeight, plusView.measuredHeight, foregroundView.measuredHeight) +
          paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      backgroundView.layout(paddingStart, paddingTop)
      plusView.layout(
        backgroundView.right + backgroundToPlusGap,
        previewCenterOffset - plusView.measuredHeight / 2
      )
      foregroundView.layout(plusView.right + plusToForegroundGap, paddingTop)
    }
  }

  private class LayerPreviewView(
    context: Context,
    drawable: Drawable,
    label: CharSequence,
    actionDescription: CharSequence,
    onClick: () -> Unit
  ) : AViewGroup(context) {

    private val labelGap = LABEL_GAP_DP.dp
    private var previewSize = DEFAULT_PREVIEW_SIZE_DP.dp
    private val iconView = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(previewSize, previewSize)
      scaleType = ImageView.ScaleType.FIT_CENTER
      setImageDrawable(drawable)
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val labelView = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        previewSize,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      text = label
      gravity = Gravity.CENTER
      ellipsize = TextUtils.TruncateAt.END
      maxLines = 1
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    init {
      isClickable = true
      isFocusable = true
      contentDescription = actionDescription
      foreground = context.createRoundedRipple(PREVIEW_RADIUS_DP.dp.toFloat())
      setOnClickListener { onClick() }
      addView(iconView)
      addView(labelView)
    }

    fun setPreviewSize(size: Int) {
      previewSize = size
      iconView.layoutParams = LayoutParams(size, size)
      labelView.layoutParams = LayoutParams(size, ViewGroup.LayoutParams.WRAP_CONTENT)
      requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      iconView.autoMeasure()
      labelView.autoMeasure()
      setMeasuredDimension(
        previewSize,
        iconView.measuredHeight + labelGap + labelView.measuredHeight
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      iconView.layout((measuredWidth - iconView.measuredWidth) / 2, 0)
      labelView.layout(
        (measuredWidth - labelView.measuredWidth) / 2,
        iconView.bottom + labelGap
      )
    }
  }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createMaskedBackgroundDrawable(icon: AdaptiveIconDrawable): Drawable {
  return MaskedBackgroundDrawable(
    background = icon.background.copyDrawable(),
    mask = Path(icon.iconMask),
    outsideAlpha = BACKGROUND_OUTSIDE_ALPHA
  )
}

@RequiresApi(Build.VERSION_CODES.O)
private class MaskedBackgroundDrawable(
  private val background: Drawable,
  private val mask: Path,
  private val outsideAlpha: Int
) : Drawable() {

  private val targetBounds = RectF()
  private val matrix = Matrix()
  private val transformedMask = Path()
  private var drawableAlpha = 255

  override fun draw(canvas: Canvas) {
    background.bounds = bounds
    if (bounds.width() <= 0 || bounds.height() <= 0 || mask.isEmpty) {
      background.alpha = drawableAlpha
      background.draw(canvas)
      return
    }

    targetBounds.set(bounds)
    matrix.reset()
    matrix.setRectToRect(ADAPTIVE_ICON_MASK_VIEWPORT, targetBounds, Matrix.ScaleToFit.CENTER)
    transformedMask.reset()
    mask.transform(matrix, transformedMask)

    val outsideSaveCount = canvas.save()
    canvas.clipOutPath(transformedMask)
    background.alpha = drawableAlpha * outsideAlpha / 255
    background.draw(canvas)
    canvas.restoreToCount(outsideSaveCount)

    val insideSaveCount = canvas.save()
    canvas.clipPath(transformedMask)
    background.alpha = drawableAlpha
    background.draw(canvas)
    canvas.restoreToCount(insideSaveCount)
  }

  override fun setAlpha(alpha: Int) {
    drawableAlpha = alpha
    invalidateSelf()
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    background.colorFilter = colorFilter
    invalidateSelf()
  }

  @Suppress("OVERRIDE_DEPRECATION")
  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }

  override fun getIntrinsicWidth(): Int {
    return background.intrinsicWidth
  }

  override fun getIntrinsicHeight(): Int {
    return background.intrinsicHeight
  }
}

private fun Context.createRoundedRipple(radius: Float): Drawable {
  val mask = createRoundedRect(Color.WHITE, radius)
  return RippleDrawable(
    ColorStateList.valueOf(getColorByAttr(android.R.attr.colorControlHighlight)),
    null,
    mask
  )
}

private fun createRoundedRect(color: Int, radius: Float): Drawable {
  return GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = radius
    setColor(color)
  }
}

private fun Drawable.copyDrawable(): Drawable {
  return constantState?.newDrawable()?.mutate() ?: mutate()
}

private val ADAPTIVE_ICON_MASK_VIEWPORT = RectF(0f, 0f, 100f, 100f)
private const val CARD_RADIUS_DP = 14
private const val CARD_ELEVATION_DP = 2
private const val CONTENT_PADDING_DP = 14
private const val STITCH_INSET_DP = 5
private const val STITCH_RADIUS_DP = 9
private const val STITCH_STROKE_WIDTH_DP = 1
private const val STITCH_DASH_LENGTH_DP = 4
private const val STITCH_DASH_GAP_DP = 4
private const val STITCH_ALPHA = 0xB3

// Foreground layers already include a safe-zone inset, so their geometric gap can stay smaller.
private const val BACKGROUND_TO_PLUS_GAP_DP = 20
private const val PLUS_TO_FOREGROUND_GAP_DP = 0
private const val PLUS_SIZE_DP = 24
private const val DEFAULT_PREVIEW_SIZE_DP = 60
private const val MIN_PREVIEW_SIZE_DP = 44
private const val BACKGROUND_OUTSIDE_ALPHA = 0x1A
private const val LABEL_GAP_DP = 2
private const val PREVIEW_RADIUS_DP = 12
