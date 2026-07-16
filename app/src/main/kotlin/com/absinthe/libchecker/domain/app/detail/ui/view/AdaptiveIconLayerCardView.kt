package com.absinthe.libchecker.domain.app.detail.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
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
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

@RequiresApi(Build.VERSION_CODES.O)
class AdaptiveIconLayerCardView(
  context: Context,
  icon: AdaptiveIconDrawable,
  onBackgroundClick: () -> Unit,
  onForegroundClick: () -> Unit
) : AViewGroup(context) {

  private val bubbleTailWidth = BUBBLE_TAIL_WIDTH_DP.dp
  private val bubblePath = Path()
  private val stitchPath = Path()
  private val bubbleBodyBounds = RectF()
  private val stitchBodyBounds = RectF()
  private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    color = context.getColorByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh)
  }
  private val stitchInset = STITCH_INSET_DP.dp.toFloat()
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
  private val maskedBackgroundDrawable = createMaskedBackgroundDrawable(icon)
  private var backgroundOutsideDisintegrationAnimator: ValueAnimator? = null
  private val contentView = ContentView(
    context = context,
    icon = icon,
    maskedBackgroundDrawable = maskedBackgroundDrawable,
    onBackgroundClick = onBackgroundClick,
    onForegroundClick = onForegroundClick
  )

  init {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    clipChildren = false
    clipToPadding = false
    contentView.layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setWillNotDraw(false)
    elevation = CARD_ELEVATION_DP.dp.toFloat()
    outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(
          bubbleTailWidth,
          0,
          view.width,
          view.height,
          CARD_RADIUS_DP.dp.toFloat()
        )
      }
    }
    isClickable = true
    isFocusable = false
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    setOnClickListener { }
    addView(contentView)
  }

  fun fitPreviewSize(desiredSize: Int, maxWidth: Int) {
    contentView.fitPreviewSize(desiredSize, (maxWidth - bubbleTailWidth).coerceAtLeast(1))
  }

  fun animateBackgroundOutsideDisintegration() {
    backgroundOutsideDisintegrationAnimator?.cancel()
    maskedBackgroundDrawable.disintegrationProgress = 0f
    backgroundOutsideDisintegrationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = BACKGROUND_DISINTEGRATION_DURATION_MS
      interpolator = LinearInterpolator()
      addUpdateListener {
        maskedBackgroundDrawable.disintegrationProgress = it.animatedValue as Float
      }
      doOnEnd {
        backgroundOutsideDisintegrationAnimator = null
      }
      start()
    }
  }

  fun finishBackgroundOutsideDisintegration() {
    backgroundOutsideDisintegrationAnimator?.cancel()
    backgroundOutsideDisintegrationAnimator = null
    maskedBackgroundDrawable.disintegrationProgress = 1f
  }

  internal val previewCenterOffset: Int
    get() = contentView.top + contentView.previewCenterOffset

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    contentView.autoMeasure()
    setMeasuredDimension(
      bubbleTailWidth + contentView.measuredWidth,
      contentView.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    contentView.layout(bubbleTailWidth, 0)
    updateBubblePaths()
  }

  override fun onDraw(canvas: Canvas) {
    canvas.drawPath(bubblePath, bubblePaint)
  }

  override fun dispatchDraw(canvas: Canvas) {
    super.dispatchDraw(canvas)
    canvas.drawPath(stitchPath, stitchPaint)
  }

  override fun onDetachedFromWindow() {
    finishBackgroundOutsideDisintegration()
    super.onDetachedFromWindow()
  }

  private fun updateBubblePaths() {
    if (width <= bubbleTailWidth || height <= 0) return
    val tailCenterY = previewCenterOffset.toFloat()
    bubbleBodyBounds.set(bubbleTailWidth.toFloat(), 0f, width.toFloat(), height.toFloat())
    bubblePath.setLayerBubble(
      bodyBounds = bubbleBodyBounds,
      tipX = 0f,
      tailCenterY = tailCenterY,
      tailTopHeight = BUBBLE_TAIL_TOP_HEIGHT_DP.dp.toFloat(),
      tailBottomHeight = BUBBLE_TAIL_BOTTOM_HEIGHT_DP.dp.toFloat(),
      radius = CARD_RADIUS_DP.dp.toFloat()
    )

    val stitchInsetWithStroke = stitchInset + stitchPaint.strokeWidth / 2f
    val stitchTail = calculateInsetTailPlacement(
      bodyLeft = bubbleTailWidth.toFloat(),
      tipX = 0f,
      centerY = tailCenterY,
      topHeight = BUBBLE_TAIL_TOP_HEIGHT_DP.dp.toFloat(),
      bottomHeight = BUBBLE_TAIL_BOTTOM_HEIGHT_DP.dp.toFloat(),
      inset = stitchInsetWithStroke,
      verticalOffset = STITCH_TAIL_Y_OFFSET_DP.dp.toFloat()
    )
    stitchBodyBounds.set(
      stitchTail.bodyLeft,
      stitchInsetWithStroke,
      width - stitchInsetWithStroke,
      height - stitchInsetWithStroke
    )
    stitchPath.setLayerBubble(
      bodyBounds = stitchBodyBounds,
      tipX = stitchTail.tipX,
      tailCenterY = stitchTail.centerY,
      tailTopHeight = stitchTail.topHeight,
      tailBottomHeight = stitchTail.bottomHeight,
      radius = STITCH_RADIUS_DP.dp.toFloat()
    )
  }

  private class ContentView(
    context: Context,
    icon: AdaptiveIconDrawable,
    maskedBackgroundDrawable: MaskedBackgroundDrawable,
    onBackgroundClick: () -> Unit,
    onForegroundClick: () -> Unit
  ) : AViewGroup(context) {

    private val contentPadding = CONTENT_PADDING_DP.dp
    private val layerGap = LAYER_GAP_DP.dp
    private var previewSize = DEFAULT_PREVIEW_SIZE_DP.dp

    val previewCenterOffset: Int
      get() = paddingTop + previewSize / 2

    private val backgroundView = LayerPreviewView(
      context = context,
      drawable = maskedBackgroundDrawable,
      label = context.getString(R.string.adaptive_icon_layer_background),
      actionDescription = context.getString(R.string.adaptive_icon_copy_background),
      onClick = onBackgroundClick
    )
    private val foregroundView = LayerPreviewView(
      context = context,
      drawable = icon.foreground.copyDrawable(),
      label = context.getString(R.string.adaptive_icon_layer_foreground),
      actionDescription = context.getString(R.string.adaptive_icon_copy_foreground),
      onClick = onForegroundClick
    )

    init {
      clipChildren = false
      clipToPadding = false
      setPadding(contentPadding, contentPadding, contentPadding, contentPadding)
      addView(backgroundView)
      addView(foregroundView)
      updatePreviewSize(previewSize)
    }

    fun fitPreviewSize(desiredSize: Int, maxWidth: Int) {
      val fixedWidth = paddingStart + paddingEnd + layerGap
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
      foregroundView.autoMeasure()
      setMeasuredDimension(
        paddingStart +
          backgroundView.measuredWidth +
          layerGap +
          foregroundView.measuredWidth +
          paddingEnd,
        paddingTop +
          maxOf(backgroundView.measuredHeight, foregroundView.measuredHeight) +
          paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      backgroundView.layout(paddingStart, paddingTop)
      foregroundView.layout(backgroundView.right + layerGap, paddingTop)
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
      clipChildren = false
      clipToPadding = false
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
private fun createMaskedBackgroundDrawable(icon: AdaptiveIconDrawable): MaskedBackgroundDrawable {
  return MaskedBackgroundDrawable(
    background = icon.background.copyDrawable(),
    mask = Path(icon.iconMask)
  )
}

@RequiresApi(Build.VERSION_CODES.O)
private class MaskedBackgroundDrawable(
  private val background: Drawable,
  private val mask: Path
) : Drawable() {

  private val targetBounds = RectF()
  private val matrix = Matrix()
  private val transformedMask = Path()
  private val sourceMaskMatrix = Matrix()
  private val sourceMaskPath = Path()
  private val sourceMaskClearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
  }
  private val particleClipPath = Path()
  private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private val particles = mutableListOf<BackgroundParticle>()
  private var sourceBitmap: Bitmap? = null
  private var drawableAlpha = 255
  var disintegrationProgress = PARTICLE_PROGRESS_IDLE
    set(value) {
      val constrainedValue = value.coerceIn(PARTICLE_PROGRESS_IDLE, 1f)
      if (field == constrainedValue) return
      field = constrainedValue
      invalidateSelf()
    }

  override fun draw(canvas: Canvas) {
    background.bounds = bounds
    background.alpha = drawableAlpha
    if (bounds.width() <= 0 || bounds.height() <= 0 || mask.isEmpty) {
      background.draw(canvas)
      return
    }

    targetBounds.set(bounds)
    matrix.reset()
    matrix.setRectToRect(ADAPTIVE_ICON_MASK_VIEWPORT, targetBounds, Matrix.ScaleToFit.CENTER)
    transformedMask.reset()
    mask.transform(matrix, transformedMask)

    if (disintegrationProgress < 0f) {
      drawStaticOutside(canvas)
    } else {
      drawDisintegratingOutside(canvas)
    }

    val insideSaveCount = canvas.save()
    canvas.clipPath(transformedMask)
    background.bounds = bounds
    background.alpha = drawableAlpha
    background.draw(canvas)
    canvas.restoreToCount(insideSaveCount)
  }

  private fun drawStaticOutside(canvas: Canvas) {
    val outsideSaveCount = canvas.save()
    canvas.clipOutPath(transformedMask)
    background.draw(canvas)
    canvas.restoreToCount(outsideSaveCount)
  }

  private fun drawDisintegratingOutside(canvas: Canvas) {
    val bitmap = ensureSourceBitmap() ?: return
    val travelDistance = min(bounds.width(), bounds.height()).toFloat()
    particles.forEach { particle ->
      val localProgress = calculateParticleLocalProgress(
        progress = disintegrationProgress,
        normalizedX = particle.normalizedX,
        activationJitter = particle.activationJitter
      )
      val alpha = calculateParticleAlpha(localProgress)
      if (alpha <= 0f) return@forEach

      val progressSquared = localProgress * localProgress
      val horizontalDirection = if (particle.velocityX >= 0f) 1f else -1f
      val offsetX = particle.velocityX * localProgress +
        horizontalDirection * travelDistance * PARTICLE_HORIZONTAL_ACCELERATION * progressSquared
      val offsetY = particle.velocityY * localProgress -
        travelDistance * PARTICLE_UPWARD_ACCELERATION * progressSquared
      val scale = 1f - PARTICLE_SCALE_REDUCTION * localProgress
      val roundness = calculateParticleRoundness(localProgress)
      particlePaint.alpha = (drawableAlpha * alpha).roundToInt()

      val particleSaveCount = canvas.save()
      canvas.translate(offsetX, offsetY)
      canvas.rotate(
        particle.rotationDegrees * localProgress,
        particle.destination.centerX(),
        particle.destination.centerY()
      )
      canvas.scale(
        scale,
        scale,
        particle.destination.centerX(),
        particle.destination.centerY()
      )
      if (roundness > 0f) {
        val cornerRadius = min(
          particle.destination.width(),
          particle.destination.height()
        ) * 0.5f * roundness
        particleClipPath.reset()
        particleClipPath.addRoundRect(
          particle.destination,
          cornerRadius,
          cornerRadius,
          Path.Direction.CW
        )
        canvas.clipPath(particleClipPath)
      }
      canvas.drawBitmap(bitmap, particle.source, particle.destination, particlePaint)
      canvas.restoreToCount(particleSaveCount)
    }
  }

  private fun ensureSourceBitmap(): Bitmap? {
    val width = bounds.width()
    val height = bounds.height()
    if (width <= 0 || height <= 0) return null
    sourceBitmap?.let { bitmap ->
      if (bitmap.width == width && bitmap.height == height) return bitmap
      bitmap.recycle()
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val bitmapCanvas = Canvas(bitmap)
    background.bounds = Rect(0, 0, width, height)
    background.alpha = FULL_ALPHA
    background.draw(bitmapCanvas)
    sourceMaskMatrix.reset()
    sourceMaskMatrix.setRectToRect(
      ADAPTIVE_ICON_MASK_VIEWPORT,
      RectF(0f, 0f, width.toFloat(), height.toFloat()),
      Matrix.ScaleToFit.CENTER
    )
    sourceMaskPath.reset()
    mask.transform(sourceMaskMatrix, sourceMaskPath)
    bitmapCanvas.drawPath(sourceMaskPath, sourceMaskClearPaint)
    sourceBitmap = bitmap
    createParticles(width, height)
    return bitmap
  }

  private fun createParticles(width: Int, height: Int) {
    particles.clear()
    val travelDistance = min(width, height).toFloat()
    repeat(PARTICLE_GRID_SIZE) { row ->
      repeat(PARTICLE_GRID_SIZE) { column ->
        val left = column * width / PARTICLE_GRID_SIZE
        val top = row * height / PARTICLE_GRID_SIZE
        val right = (column + 1) * width / PARTICLE_GRID_SIZE
        val bottom = (row + 1) * height / PARTICLE_GRID_SIZE
        val normalizedX = (left + right) / 2f / width
        val direction = particleRandom(column, row, 0) * (2f * PI.toFloat())
        val speed = travelDistance * (
          PARTICLE_MIN_SPEED +
            (PARTICLE_MAX_SPEED - PARTICLE_MIN_SPEED) * particleRandom(column, row, 1)
          )
        particles += BackgroundParticle(
          source = Rect(left, top, right, bottom),
          destination = RectF(
            bounds.left + left.toFloat(),
            bounds.top + top.toFloat(),
            bounds.left + right.toFloat(),
            bounds.top + bottom.toFloat()
          ),
          normalizedX = normalizedX,
          activationJitter = (
            particleRandom(column, row, 2) - 0.5f
            ) * PARTICLE_ACTIVATION_JITTER,
          velocityX = cos(direction) * speed,
          velocityY = sin(direction) * speed,
          rotationDegrees = (
            particleRandom(column, row, 3) - 0.5f
            ) * PARTICLE_ROTATION_RANGE_DEGREES
        )
      }
    }
  }

  override fun setAlpha(alpha: Int) {
    drawableAlpha = alpha.coerceIn(0, FULL_ALPHA)
    invalidateSelf()
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    background.colorFilter = colorFilter
    sourceBitmap?.recycle()
    sourceBitmap = null
    particles.clear()
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

  private data class BackgroundParticle(
    val source: Rect,
    val destination: RectF,
    val normalizedX: Float,
    val activationJitter: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotationDegrees: Float
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

internal data class LayerBubbleTailPlacement(
  val bodyLeft: Float,
  val tipX: Float,
  val centerY: Float,
  val topHeight: Float,
  val bottomHeight: Float
)

internal fun calculateInsetTailPlacement(
  bodyLeft: Float,
  tipX: Float,
  centerY: Float,
  topHeight: Float,
  bottomHeight: Float,
  inset: Float,
  verticalOffset: Float
): LayerBubbleTailPlacement {
  return LayerBubbleTailPlacement(
    bodyLeft = bodyLeft + inset,
    tipX = tipX + inset,
    centerY = centerY + verticalOffset,
    topHeight = topHeight,
    bottomHeight = bottomHeight
  )
}

internal fun calculateParticleLocalProgress(
  progress: Float,
  normalizedX: Float,
  activationJitter: Float
): Float {
  val constrainedProgress = progress.coerceIn(0f, 1f)
  val activationPoint = (
    normalizedX.coerceIn(0f, 1f) * PARTICLE_ACTIVATION_SPREAD + activationJitter
    ).coerceIn(0f, PARTICLE_MAX_ACTIVATION_POINT)
  return ((constrainedProgress - activationPoint) / (1f - activationPoint)).coerceIn(0f, 1f)
}

internal fun calculateParticleAlpha(localProgress: Float): Float {
  return (1f - localProgress.coerceIn(0f, 1f)).pow(PARTICLE_ALPHA_EXPONENT)
}

internal fun calculateParticleRoundness(localProgress: Float): Float {
  val fraction = (
    localProgress.coerceIn(0f, 1f) / PARTICLE_ROUNDING_END_PROGRESS
    ).coerceIn(0f, 1f)
  return fraction * fraction * (3f - 2f * fraction)
}

private fun particleRandom(column: Int, row: Int, salt: Int): Float {
  var value = column * 73_856_093 xor row * 19_349_663 xor salt * 83_492_791
  value = value xor (value shl 13)
  value = value xor (value ushr 17)
  value = value xor (value shl 5)
  return (value and Int.MAX_VALUE) / Int.MAX_VALUE.toFloat()
}

private fun Path.setLayerBubble(
  bodyBounds: RectF,
  tipX: Float,
  tailCenterY: Float,
  tailTopHeight: Float,
  tailBottomHeight: Float,
  radius: Float
) {
  reset()
  val cornerRadius = min(radius, min(bodyBounds.width(), bodyBounds.height()) / 2f)
  val tailWidth = bodyBounds.left - tipX
  val tailShoulderTop = tailCenterY - tailTopHeight
  val tailShoulderBottom = tailCenterY + tailBottomHeight
  val tailArcRadiusX = tailWidth / (1f - TAIL_ARC_SWEEP_COS)
  val tailArcRadiusY = tailTopHeight / TAIL_ARC_SWEEP_SIN
  val tailArcCenterY = tailShoulderTop
  val tailArcBounds = RectF(
    bodyBounds.left - tailArcRadiusX * 2f,
    tailArcCenterY - tailArcRadiusY,
    bodyBounds.left,
    tailArcCenterY + tailArcRadiusY
  )

  moveTo(bodyBounds.left + cornerRadius, bodyBounds.top)
  lineTo(bodyBounds.right - cornerRadius, bodyBounds.top)
  quadTo(bodyBounds.right, bodyBounds.top, bodyBounds.right, bodyBounds.top + cornerRadius)
  lineTo(bodyBounds.right, bodyBounds.bottom - cornerRadius)
  quadTo(bodyBounds.right, bodyBounds.bottom, bodyBounds.right - cornerRadius, bodyBounds.bottom)
  lineTo(bodyBounds.left + cornerRadius, bodyBounds.bottom)
  quadTo(bodyBounds.left, bodyBounds.bottom, bodyBounds.left, bodyBounds.bottom - cornerRadius)
  lineTo(bodyBounds.left, tailShoulderBottom)
  cubicTo(
    bodyBounds.left,
    tailCenterY + tailBottomHeight * 0.72f,
    tipX + tailWidth * 0.32f,
    tailCenterY + tailBottomHeight * 0.52f,
    tipX,
    tailCenterY
  )
  arcTo(tailArcBounds, TAIL_ARC_SWEEP_DEGREES, -TAIL_ARC_SWEEP_DEGREES, false)
  lineTo(bodyBounds.left, bodyBounds.top + cornerRadius)
  quadTo(bodyBounds.left, bodyBounds.top, bodyBounds.left + cornerRadius, bodyBounds.top)
  close()
}

private val ADAPTIVE_ICON_MASK_VIEWPORT = RectF(0f, 0f, 100f, 100f)
private const val CARD_RADIUS_DP = 14
private const val CARD_ELEVATION_DP = 2
private const val BUBBLE_TAIL_WIDTH_DP = 18
private const val BUBBLE_TAIL_TOP_HEIGHT_DP = 6
private const val BUBBLE_TAIL_BOTTOM_HEIGHT_DP = 17
private const val TAIL_ARC_SWEEP_DEGREES = 83f
private const val TAIL_ARC_SWEEP_COS = 0.12186934f
private const val TAIL_ARC_SWEEP_SIN = 0.99254614f
private const val CONTENT_PADDING_DP = 14
private const val STITCH_INSET_DP = 5
private const val STITCH_RADIUS_DP = 9
private const val STITCH_STROKE_WIDTH_DP = 1
private const val STITCH_DASH_LENGTH_DP = 4
private const val STITCH_DASH_GAP_DP = 4
private const val STITCH_ALPHA = 0xB3
private const val STITCH_TAIL_Y_OFFSET_DP = 2
private const val LAYER_GAP_DP = 20
private const val DEFAULT_PREVIEW_SIZE_DP = 60
private const val MIN_PREVIEW_SIZE_DP = 44
private const val FULL_ALPHA = 0xFF
private const val PARTICLE_PROGRESS_IDLE = -1f
private const val BACKGROUND_DISINTEGRATION_DURATION_MS = 1_500L
private const val PARTICLE_ROUNDING_END_PROGRESS = 0.12f
private const val PARTICLE_GRID_SIZE = 18
private const val PARTICLE_ACTIVATION_SPREAD = 0.24f
private const val PARTICLE_ACTIVATION_JITTER = 0.06f
private const val PARTICLE_MAX_ACTIVATION_POINT = 0.3f
private const val PARTICLE_MIN_SPEED = 0.1f
private const val PARTICLE_MAX_SPEED = 0.3f
private const val PARTICLE_HORIZONTAL_ACCELERATION = 0.18f
private const val PARTICLE_UPWARD_ACCELERATION = 0.22f
private const val PARTICLE_SCALE_REDUCTION = 0.35f
private const val PARTICLE_ROTATION_RANGE_DEGREES = 52f
private const val PARTICLE_ALPHA_EXPONENT = 1.45f
private const val LABEL_GAP_DP = 2
private const val PREVIEW_RADIUS_DP = 12
