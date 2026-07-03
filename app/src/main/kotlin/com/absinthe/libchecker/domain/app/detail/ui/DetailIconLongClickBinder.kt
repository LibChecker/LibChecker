package com.absinthe.libchecker.domain.app.detail.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
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
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnPreDraw
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.copyBitmapToClipboard
import com.absinthe.libchecker.utils.extensions.copyToClipboard
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr

fun ImageView.setDetailIconLongClick(applicationInfo: ApplicationInfo?, blurView: View) {
  setOnLongClickListener {
    if (!OsUtils.atLeastO()) {
      copyToClipboard()
      return@setOnLongClickListener true
    }
    val adaptiveIcon = applicationInfo?.loadAdaptiveIconOnO(context)
    if (adaptiveIcon == null) {
      copyToClipboard()
    } else {
      showAdaptiveIconLayerOverlay(adaptiveIcon, blurView)
    }
    true
  }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ImageView.showAdaptiveIconLayerOverlay(icon: AdaptiveIconDrawable, blurView: View) {
  val activity = context.findActivity()
  if (activity == null) {
    copyToClipboard()
    return
  }
  AdaptiveIconLayerOverlay(activity, icon, this, blurView).show()
}

private tailrec fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ApplicationInfo.loadAdaptiveIconOnO(context: Context): AdaptiveIconDrawable? {
  return loadIcon(context.packageManager) as? AdaptiveIconDrawable
}

@RequiresApi(Build.VERSION_CODES.O)
private class AdaptiveIconLayerOverlay(
  private val activity: Activity,
  private val icon: AdaptiveIconDrawable,
  private val sourceView: ImageView,
  private val blurView: View
) {

  private val context = activity
  private var itemSize = sourceView.width.takeIf { it > 0 } ?: 56.dp
  private val layerGap = 8.dp
  private val edgePadding = 24.dp
  private val maxBlurRadius = 24f
  private val originalBlurViewAlpha = blurView.alpha
  private var blurAnimator: ValueAnimator? = null
  private var blurMaskShader: RuntimeShader? = null
  private var currentBlurRadius = 0f
  private var isClosing = false
  private var collapseX = 0f
  private var collapseY = 0f
  private val overlay = FrameLayout(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    setBackgroundColor(Color.TRANSPARENT)
    isClickable = true
    setOnClickListener { close() }
  }
  private val originalView = createIconView(sourceView.drawable?.copyDrawable() ?: icon.copyDrawable())
  private val equalsView = createIconView(
    context.getDrawableCompat(R.drawable.ic_equal)
  ).apply {
    alpha = 0f
    setOnClickListener { }
    setPadding(17.dp, 17.dp, 17.dp, 17.dp)
  }
  private val backgroundView = createIconView(createBackgroundOutlineDrawable()).apply {
    alpha = 0f
  }
  private val plusView = createIconView(
    context.getDrawableCompat(R.drawable.ic_add)
  ).apply {
    alpha = 0f
    foreground = context.createCircleRipple()
    isFocusable = true
    setPadding(17.dp, 17.dp, 17.dp, 17.dp)
  }
  private val foregroundView = createIconView(icon.foreground.copyDrawable()).apply {
    alpha = 0f
  }
  private val layerViews = listOf(
    originalView,
    equalsView,
    backgroundView,
    plusView,
    foregroundView
  )

  init {
    layerViews.forEach(overlay::addView)
    bindLayerClickActions()
  }

  fun show() {
    val decorView = activity.window.decorView as? ViewGroup ?: return
    decorView.addView(overlay)
    overlay.doOnPreDraw { startLayerAnimation() }
  }

  private fun bindLayerClickActions() {
    originalView.setOnClickListener {
      copyFullIcon()
    }
    backgroundView.setOnClickListener {
      context.copyBitmapToClipboard(icon.background.toBitmap(itemSize, itemSize))
    }
    plusView.setOnClickListener {
      copyFullIcon()
    }
    foregroundView.setOnClickListener {
      context.copyBitmapToClipboard(icon.foreground.toBitmap(itemSize, itemSize))
    }
  }

  private fun createIconView(drawable: Drawable): AppCompatImageView {
    return AppCompatImageView(context).apply {
      layoutParams = FrameLayout.LayoutParams(itemSize, itemSize)
      scaleType = ImageView.ScaleType.FIT_CENTER
      setImageDrawable(drawable)
      isClickable = true
    }
  }

  private fun createBackgroundOutlineDrawable(): Drawable {
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

  private fun startLayerAnimation() {
    val sourcePosition = resolveSourcePosition()
    collapseX = sourcePosition.x
    collapseY = sourcePosition.y

    applyBlur()
    updateLayerSize()
    placeLayersAt(sourcePosition.x, sourcePosition.y)
    animateLayerRow(sourcePosition.x, sourcePosition.y)
  }

  private fun resolveSourcePosition(): LayerPosition {
    val rootLocation = IntArray(2)
    val sourceLocation = IntArray(2)
    overlay.getLocationOnScreen(rootLocation)
    sourceView.getLocationOnScreen(sourceLocation)
    return LayerPosition(
      x = (sourceLocation[0] - rootLocation[0]).toFloat(),
      y = (sourceLocation[1] - rootLocation[1]).toFloat()
    )
  }

  private fun updateLayerSize() {
    itemSize = sourceView.width.coerceAtLeast(1)
    layerViews.forEach { child ->
      child.layoutParams = FrameLayout.LayoutParams(itemSize, itemSize)
    }
  }

  private fun placeLayersAt(x: Float, y: Float) {
    layerViews.forEach { child ->
      child.x = x
      child.y = y
      child.scaleX = 1f
      child.scaleY = 1f
    }
  }

  private fun animateLayerRow(startX: Float, rowY: Float) {
    val rowWidth = itemSize * layerViews.size + layerGap * (layerViews.size - 1)
    val rowStartX = if (startX + rowWidth <= overlay.width - edgePadding) {
      startX
    } else {
      (overlay.width - rowWidth - edgePadding).coerceAtLeast(edgePadding).toFloat()
    }

    layerViews.forEachIndexed { index, view ->
      animateTo(view, rowStartX + (itemSize + layerGap) * index, rowY, 1f)
    }
  }

  private fun animateTo(
    view: View,
    x: Float,
    y: Float,
    endAlpha: Float
  ) {
    view.animate()
      .x(x)
      .y(y)
      .alpha(endAlpha)
      .setDuration(ANIMATION_DURATION_MS)
      .setInterpolator(FastOutSlowInInterpolator())
      .start()
  }

  private fun applyBlur() {
    if (shouldHideCollapsingToolbarInsteadOfBlur()) {
      animateBlurViewAlpha(0f)
      return
    }
    blurAnimator = ValueAnimator.ofFloat(0f, maxBlurRadius).apply {
      duration = ANIMATION_DURATION_MS
      interpolator = FastOutSlowInInterpolator()
      addUpdateListener {
        updateBlurRadius(it.animatedValue as Float)
      }
      start()
    }
  }

  private fun close() {
    if (isClosing) return
    isClosing = true
    collapseLayers()
    blurAnimator?.cancel()
    if (!shouldHideCollapsingToolbarInsteadOfBlur() && currentBlurRadius > 0f) {
      blurAnimator = ValueAnimator.ofFloat(currentBlurRadius, 0f).apply {
        duration = ANIMATION_DURATION_MS
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener {
          updateBlurRadius(it.animatedValue as Float)
        }
        doOnEnd {
          updateBlurRadius(0f)
          removeOverlay()
        }
        start()
      }
    } else {
      animateBlurViewAlpha(originalBlurViewAlpha) {
        removeOverlay()
      }
    }
  }

  private fun collapseLayers() {
    originalView.bringToFront()
    collapseLayer(originalView, ANIMATION_DURATION_MS, 1f)
    layerViews.drop(1).forEach { child ->
      collapseLayer(child, COLLAPSING_LAYER_FADE_DURATION_MS, 0f)
    }
  }

  private fun collapseLayer(view: View, duration: Long, endAlpha: Float) {
    view.animate().cancel()
    view.animate()
      .x(collapseX)
      .y(collapseY)
      .alpha(endAlpha)
      .setDuration(duration)
      .setInterpolator(FastOutSlowInInterpolator())
      .start()
  }

  private fun updateBlurRadius(radius: Float) {
    currentBlurRadius = radius
    if (shouldHideCollapsingToolbarInsteadOfBlur()) return
    updateBlurView(blurView, radius)
  }

  private fun removeOverlay() {
    blurAnimator = null
    (overlay.parent as? ViewGroup)?.removeView(overlay)
  }

  private fun copyFullIcon() {
    context.copyBitmapToClipboard(icon.toBitmap(itemSize, itemSize))
  }

  private fun animateBlurViewAlpha(alpha: Float, endAction: (() -> Unit)? = null) {
    blurView.animate().cancel()
    blurView.animate()
      .alpha(alpha)
      .setDuration(ANIMATION_DURATION_MS)
      .setInterpolator(FastOutSlowInInterpolator())
      .withEndAction {
        endAction?.invoke()
      }
      .start()
  }

  private fun updateBlurView(view: View, radius: Float) {
    if (!OsUtils.atLeastS()) return
    view.setRenderEffect(
      if (radius > 0f) {
        if (OsUtils.atLeastT()) {
          createProgressiveBlurEffectOnT(view, radius)
        } else {
          RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
        }
      } else {
        null
      }
    )
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun createProgressiveBlurEffectOnT(view: View, radius: Float): RenderEffect {
    val shader = blurMaskShader ?: RuntimeShader(PROGRESSIVE_BLUR_MASK_SHADER).also {
      blurMaskShader = it
    }
    shader.setFloatUniform("size", view.width.toFloat(), view.height.toFloat())
    shader.setFloatUniform("progress", (radius / maxBlurRadius).coerceIn(0f, 1f))
    return RenderEffect.createChainEffect(
      RenderEffect.createRuntimeShaderEffect(shader, "content"),
      RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
    )
  }
}

private data class LayerPosition(
  val x: Float,
  val y: Float
)

private fun shouldHideCollapsingToolbarInsteadOfBlur(): Boolean {
  return !OsUtils.atLeastT()
}

private const val ANIMATION_DURATION_MS = 350L
private const val COLLAPSING_LAYER_FADE_DURATION_MS = 180L

private const val PROGRESSIVE_BLUR_MASK_SHADER = """
uniform shader content;
uniform float2 size;
uniform float progress;

half4 main(float2 coord) {
  float fadeStartY = size.y * 0.8;
  float maskAlpha = 1.0 - smoothstep(fadeStartY, size.y, coord.y);
  return content.eval(coord) * mix(1.0, maskAlpha, progress);
}
"""

private class IconMaskOutlineDrawable(
  private val background: Drawable,
  private val mask: Path,
  outlineColor: Int,
  strokeWidth: Float,
  dashLength: Float,
  dashGap: Float
) : Drawable() {

  private val sourceBounds = RectF()
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

    mask.computeBounds(sourceBounds, true)
    if (sourceBounds.width() <= 0f || sourceBounds.height() <= 0f) return

    targetBounds.set(bounds)
    val inset = outlinePaint.strokeWidth / 2f
    targetBounds.inset(inset, inset)

    matrix.reset()
    matrix.setRectToRect(sourceBounds, targetBounds, Matrix.ScaleToFit.CENTER)
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

private fun Drawable.chooseVisibleOutlineColor(): Int {
  val bitmap = runCatching { toBitmap(24, 24) }.getOrNull() ?: return Color.WHITE
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
  if (alphaSum == 0L) return Color.WHITE

  val averageColor = Color.rgb(
    (redSum / alphaSum).toInt(),
    (greenSum / alphaSum).toInt(),
    (blueSum / alphaSum).toInt()
  )
  val outlineColor = if (ColorUtils.calculateLuminance(averageColor) > 0.5) {
    Color.BLACK
  } else {
    Color.WHITE
  }
  return ColorUtils.setAlphaComponent(outlineColor, 0xE6)
}

private fun Drawable.copyDrawable(): Drawable {
  return constantState?.newDrawable()?.mutate() ?: mutate()
}

private fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable {
  return requireNotNull(ContextCompat.getDrawable(this, resId)).mutate()
}

private fun Context.createCircleRipple(): Drawable {
  val mask = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(Color.WHITE)
  }
  return RippleDrawable(
    ColorStateList.valueOf(getColorByAttr(android.R.attr.colorControlHighlight)),
    null,
    mask
  )
}
