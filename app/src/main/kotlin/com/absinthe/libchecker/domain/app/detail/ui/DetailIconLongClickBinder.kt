package com.absinthe.libchecker.domain.app.detail.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.graphics.Color
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
    val adaptiveIcon = applicationInfo?.loadAdaptiveIcon(context)
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

private fun ApplicationInfo.loadAdaptiveIcon(context: Context): AdaptiveIconDrawable? {
  if (!OsUtils.atLeastO()) return null
  return loadAdaptiveIconOnO(context)
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
  private val gap = 8.dp
  private val maxBlurRadius = 24f
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
  private val backgroundView = createIconView(icon.background.copyDrawable()).apply {
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

  init {
    overlay.addView(originalView)
    overlay.addView(equalsView)
    overlay.addView(backgroundView)
    overlay.addView(plusView)
    overlay.addView(foregroundView)

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

  fun show() {
    val decorView = activity.window.decorView as? ViewGroup ?: return
    decorView.addView(overlay)
    overlay.doOnPreDraw { startLayerAnimation() }
  }

  private fun createIconView(drawable: Drawable): AppCompatImageView {
    return AppCompatImageView(context).apply {
      layoutParams = FrameLayout.LayoutParams(itemSize, itemSize)
      scaleType = ImageView.ScaleType.FIT_CENTER
      setImageDrawable(drawable)
      isClickable = true
    }
  }

  private fun startLayerAnimation() {
    val rootLocation = IntArray(2)
    val sourceLocation = IntArray(2)
    overlay.getLocationOnScreen(rootLocation)
    sourceView.getLocationOnScreen(sourceLocation)

    applyBlur()

    itemSize = sourceView.width.coerceAtLeast(1)
    listOf(originalView, equalsView, backgroundView, plusView, foregroundView).forEach { child ->
      child.layoutParams = FrameLayout.LayoutParams(itemSize, itemSize)
    }

    val startX = (sourceLocation[0] - rootLocation[0]).toFloat()
    val startY = (sourceLocation[1] - rootLocation[1]).toFloat()
    collapseX = startX
    collapseY = startY
    val scale = 1f

    val rowWidth = itemSize * 5 + gap * 4
    val rowStartX = if (startX + rowWidth <= overlay.width - 24.dp) {
      startX
    } else {
      (overlay.width - rowWidth - 24.dp).coerceAtLeast(24.dp).toFloat()
    }
    val rowY = startY

    listOf(originalView, equalsView, backgroundView, plusView, foregroundView).forEach { child ->
      child.x = startX
      child.y = startY
      child.scaleX = scale
      child.scaleY = scale
    }

    animateTo(originalView, rowStartX, rowY, scale, 1f)
    animateTo(equalsView, rowStartX + itemSize + gap, rowY, scale, 1f)
    animateTo(backgroundView, rowStartX + (itemSize + gap) * 2, rowY, scale, 1f)
    animateTo(plusView, rowStartX + (itemSize + gap) * 3, rowY, scale, 1f)
    animateTo(foregroundView, rowStartX + (itemSize + gap) * 4, rowY, scale, 1f)
  }

  private fun animateTo(
    view: View,
    x: Float,
    y: Float,
    startScale: Float,
    endAlpha: Float
  ) {
    view.animate()
      .x(x)
      .y(y)
      .scaleX(1f)
      .scaleY(1f)
      .alpha(endAlpha)
      .setDuration(350L)
      .setInterpolator(FastOutSlowInInterpolator())
      .withStartAction {
        view.scaleX = startScale
        view.scaleY = startScale
      }
      .start()
  }

  private fun applyBlur() {
    if (!OsUtils.atLeastS()) return
    blurAnimator = ValueAnimator.ofFloat(0f, maxBlurRadius).apply {
      duration = 350L
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
    if (OsUtils.atLeastS() && currentBlurRadius > 0f) {
      blurAnimator = ValueAnimator.ofFloat(currentBlurRadius, 0f).apply {
        duration = 350L
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
      overlay.postDelayed({ removeOverlay() }, 350L)
    }
  }

  private fun collapseLayers() {
    listOf(originalView, equalsView, backgroundView, plusView, foregroundView).forEach { child ->
      child.animate()
        .x(collapseX)
        .y(collapseY)
        .alpha(0f)
        .setDuration(350L)
        .setInterpolator(FastOutSlowInInterpolator())
        .start()
    }
  }

  private fun updateBlurRadius(radius: Float) {
    currentBlurRadius = radius
    if (!OsUtils.atLeastS()) return
    updateBlurView(blurView, radius)
  }

  private fun removeOverlay() {
    blurAnimator = null
    (overlay.parent as? ViewGroup)?.removeView(overlay)
  }

  private fun copyFullIcon() {
    context.copyBitmapToClipboard(icon.toBitmap(itemSize, itemSize))
  }

  private fun updateBlurView(view: View, radius: Float) {
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
