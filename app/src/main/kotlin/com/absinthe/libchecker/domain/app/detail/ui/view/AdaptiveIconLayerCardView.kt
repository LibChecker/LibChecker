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
import androidx.core.graphics.drawable.toBitmap
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

  private class ContentView(
    context: Context,
    icon: AdaptiveIconDrawable,
    onBackgroundClick: () -> Unit,
    onForegroundClick: () -> Unit
  ) : AViewGroup(context) {

    private val contentPadding = CONTENT_PADDING_DP.dp
    private val layerGap = LAYER_GAP_DP.dp
    private val plusSize = PLUS_SIZE_DP.dp
    private var previewSize = DEFAULT_PREVIEW_SIZE_DP.dp

    private val backgroundView = LayerPreviewView(
      context = context,
      drawable = createBackgroundOutlineDrawable(icon),
      label = context.getString(R.string.adaptive_icon_layer_background),
      actionDescription = context.getString(R.string.adaptive_icon_copy_background),
      previewBackgroundColor = null,
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
      previewBackgroundColor = icon.foreground.choosePreviewSurfaceColor(context),
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
      val fixedWidth = paddingStart + paddingEnd + plusSize + layerGap * 2
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
          layerGap +
          plusView.measuredWidth +
          layerGap +
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
        backgroundView.right + layerGap,
        (measuredHeight - plusView.measuredHeight) / 2
      )
      foregroundView.layout(plusView.right + layerGap, paddingTop)
    }
  }

  private class LayerPreviewView(
    context: Context,
    drawable: Drawable,
    label: CharSequence,
    actionDescription: CharSequence,
    previewBackgroundColor: Int?,
    onClick: () -> Unit
  ) : AViewGroup(context) {

    private val labelGap = LABEL_GAP_DP.dp
    private var previewSize = DEFAULT_PREVIEW_SIZE_DP.dp
    private val iconView = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(previewSize, previewSize)
      scaleType = ImageView.ScaleType.FIT_CENTER
      setImageDrawable(drawable)
      previewBackgroundColor?.let { color ->
        background = createRoundedRect(color, PREVIEW_RADIUS_DP.dp.toFloat())
        setPadding(PREVIEW_ICON_PADDING_DP.dp, PREVIEW_ICON_PADDING_DP.dp, PREVIEW_ICON_PADDING_DP.dp, PREVIEW_ICON_PADDING_DP.dp)
      }
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

private fun createBackgroundOutlineDrawable(icon: AdaptiveIconDrawable): Drawable {
  val background = icon.background.copyDrawable()
  return IconMaskOutlineDrawable(
    background = background,
    mask = Path(icon.iconMask),
    outlineColor = background.copyDrawable().chooseVisibleOutlineColor(),
    strokeWidth = 2.dp.toFloat(),
    dashLength = 7.dp.toFloat(),
    dashGap = 5.dp.toFloat()
  )
}

private class IconMaskOutlineDrawable(
  private val background: Drawable,
  private val mask: Path,
  outlineColor: Int,
  strokeWidth: Float,
  dashLength: Float,
  dashGap: Float
) : Drawable() {

  private val targetBounds = RectF()
  private val matrix = Matrix()
  private val outlinePath = Path()
  private val outlineAlpha = Color.alpha(outlineColor)
  private var drawableAlpha = 255
  private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    this.strokeWidth = strokeWidth
    color = outlineColor
    pathEffect = DashPathEffect(floatArrayOf(dashLength, dashGap), 0f)
  }

  override fun draw(canvas: Canvas) {
    background.bounds = bounds
    background.draw(canvas)
    if (bounds.width() <= 0 || bounds.height() <= 0 || mask.isEmpty) return

    targetBounds.set(bounds)
    val inset = outlinePaint.strokeWidth / 2f
    targetBounds.inset(inset, inset)

    matrix.reset()
    matrix.setRectToRect(ADAPTIVE_ICON_MASK_VIEWPORT, targetBounds, Matrix.ScaleToFit.CENTER)
    outlinePath.reset()
    mask.transform(matrix, outlinePath)
    outlinePaint.alpha = outlineAlpha * drawableAlpha / 255
    canvas.drawPath(outlinePath, outlinePaint)
  }

  override fun setAlpha(alpha: Int) {
    drawableAlpha = alpha
    background.alpha = alpha
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

private fun Drawable.choosePreviewSurfaceColor(context: Context): Int {
  val surface = context.getColorByAttr(com.google.android.material.R.attr.colorSurface)
  val inverseSurface = context.getColorByAttr(com.google.android.material.R.attr.colorSurfaceInverse)
  val averageColor = calculateAverageColor() ?: return inverseSurface
  return if (ColorUtils.calculateContrast(averageColor, surface) >= ColorUtils.calculateContrast(averageColor, inverseSurface)) {
    surface
  } else {
    inverseSurface
  }
}

private fun Drawable.chooseVisibleOutlineColor(): Int {
  val averageColor = calculateAverageColor() ?: return Color.WHITE
  val outlineColor = if (ColorUtils.calculateLuminance(averageColor) > 0.5) {
    Color.BLACK
  } else {
    Color.WHITE
  }
  return ColorUtils.setAlphaComponent(outlineColor, 0xE6)
}

private fun Drawable.calculateAverageColor(): Int? {
  val bitmap = runCatching { toBitmap(24, 24) }.getOrNull() ?: return null
  var alphaSum = 0L
  var redSum = 0L
  var greenSum = 0L
  var blueSum = 0L

  for (y in 0 until bitmap.height) {
    for (x in 0 until bitmap.width) {
      val color = bitmap.getPixel(x, y)
      val alpha = Color.alpha(color)
      if (alpha == 0) continue
      alphaSum += alpha
      redSum += Color.red(color) * alpha
      greenSum += Color.green(color) * alpha
      blueSum += Color.blue(color) * alpha
    }
  }
  if (alphaSum == 0L) return null

  return Color.rgb(
    (redSum / alphaSum).toInt(),
    (greenSum / alphaSum).toInt(),
    (blueSum / alphaSum).toInt()
  )
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
private const val CONTENT_PADDING_DP = 8
private const val LAYER_GAP_DP = 8
private const val PLUS_SIZE_DP = 24
private const val DEFAULT_PREVIEW_SIZE_DP = 60
private const val MIN_PREVIEW_SIZE_DP = 44
private const val LABEL_GAP_DP = 2
private const val PREVIEW_RADIUS_DP = 12
private const val PREVIEW_ICON_PADDING_DP = 4
