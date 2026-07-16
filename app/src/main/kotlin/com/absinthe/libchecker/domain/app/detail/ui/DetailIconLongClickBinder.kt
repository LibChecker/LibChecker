package com.absinthe.libchecker.domain.app.detail.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
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
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnPreDraw
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.ui.view.AdaptiveIconLayerCardView
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.copyBitmapToClipboard
import com.absinthe.libchecker.utils.extensions.copyToClipboard
import com.absinthe.libchecker.utils.extensions.dp

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
  private val edgePadding = 16.dp
  private val operatorSize = 24.dp
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
  private val originalView = createIconView(
    sourceView.drawable?.copyDrawable() ?: icon.copyDrawable()
  ).apply {
    contentDescription = context.getString(R.string.adaptive_icon_copy_full)
  }
  private val equalsView = createOperatorView(R.drawable.ic_equal).apply {
    alpha = 0f
    setOnClickListener { }
  }
  private val layerCardView = AdaptiveIconLayerCardView(
    context = context,
    icon = icon,
    onBackgroundClick = {
      context.copyBitmapToClipboard(icon.background.toBitmap(itemSize, itemSize))
    },
    onForegroundClick = {
      context.copyBitmapToClipboard(icon.foreground.toBitmap(itemSize, itemSize))
    }
  ).apply {
    alpha = 0f
  }
  private val layerViews = listOf(
    originalView,
    equalsView,
    layerCardView
  )

  init {
    layerViews.forEach(overlay::addView)
    bindLayerClickActions()
  }

  fun show() {
    val decorView = activity.window.decorView as? ViewGroup ?: return
    updateLayerSize(decorView.width)
    decorView.addView(overlay)
    overlay.doOnPreDraw { startLayerAnimation() }
  }

  private fun bindLayerClickActions() {
    originalView.setOnClickListener {
      copyFullIcon()
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

  private fun createOperatorView(@DrawableRes drawableRes: Int): AppCompatImageView {
    return AppCompatImageView(context).apply {
      layoutParams = FrameLayout.LayoutParams(operatorSize, operatorSize)
      scaleType = ImageView.ScaleType.CENTER_INSIDE
      setImageDrawable(context.getDrawableCompat(drawableRes))
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
      isClickable = true
      isFocusable = false
    }
  }

  private fun startLayerAnimation() {
    val sourcePosition = resolveSourcePosition()
    collapseX = sourcePosition.x
    collapseY = sourcePosition.y

    applyBlur()
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

  private fun updateLayerSize(windowWidth: Int) {
    itemSize = sourceView.width.coerceAtLeast(1)
    originalView.layoutParams = FrameLayout.LayoutParams(itemSize, itemSize)
    equalsView.layoutParams = FrameLayout.LayoutParams(operatorSize, operatorSize)
    val maxCardWidth = (
      windowWidth -
        edgePadding * 2 -
        itemSize -
        equalsView.layoutParams.width -
        layerGap * 2
      ).coerceAtLeast(1)
    layerCardView.fitPreviewSize(itemSize, maxCardWidth)
  }

  private fun placeLayersAt(x: Float, y: Float) {
    layerViews.forEach { child ->
      child.x = x
      child.y = y + (itemSize - child.height) / 2f
      val initialScale = if (child === originalView) 1f else 0.92f
      child.scaleX = initialScale
      child.scaleY = initialScale
    }
  }

  private fun animateLayerRow(startX: Float, rowY: Float) {
    val rowWidth = layerViews.sumOf { it.width } + layerGap * (layerViews.size - 1)
    val rowStartX = if (startX + rowWidth <= overlay.width - edgePadding) {
      startX
    } else {
      (overlay.width - rowWidth - edgePadding).coerceAtLeast(edgePadding).toFloat()
    }

    var nextX = rowStartX
    layerViews.forEach { view ->
      animateTo(
        view = view,
        x = nextX,
        y = rowY + (itemSize - view.height) / 2f,
        endAlpha = 1f,
        endScale = 1f
      )
      nextX += view.width + layerGap
    }
  }

  private fun animateTo(
    view: View,
    x: Float,
    y: Float,
    endAlpha: Float,
    endScale: Float
  ) {
    view.animate()
      .x(x)
      .y(y)
      .alpha(endAlpha)
      .scaleX(endScale)
      .scaleY(endScale)
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
      .y(collapseY + (itemSize - view.height) / 2f)
      .alpha(endAlpha)
      .scaleX(if (view === originalView) 1f else 0.92f)
      .scaleY(if (view === originalView) 1f else 0.92f)
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

private fun Drawable.copyDrawable(): Drawable {
  return constantState?.newDrawable()?.mutate() ?: mutate()
}

private fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable {
  return requireNotNull(ContextCompat.getDrawable(this, resId)).mutate()
}
