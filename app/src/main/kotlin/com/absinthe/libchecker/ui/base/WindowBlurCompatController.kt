package com.absinthe.libchecker.ui.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.absinthe.libchecker.utils.extensions.activity
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import java.util.WeakHashMap
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.S)
internal class WindowBlurCompatController(
  context: Context,
  private val window: Window
) {
  private val windowManager = context.getSystemService(WindowManager::class.java)
  private val mainExecutor = context.mainExecutor
  private val hostView = context.activity?.findViewById<ViewGroup>(android.R.id.content)
  private val blurInterpolator = AccelerateDecelerateInterpolator()
  private val blurEnabledListener = Consumer<Boolean> { enabled ->
    if (!isStarted || isStopRequested || isCrossWindowBlurEnabled == enabled) return@Consumer
    isCrossWindowBlurEnabled = enabled
    applyBlurRadius()
  }

  private var blurAnimator: ValueAnimator? = null
  private var blurRadius = 0f
  private var isStarted = false
  private var isStopRequested = false
  private var isCrossWindowBlurEnabled = false

  fun start() {
    if (isStopRequested) {
      stop()
    }
    if (isStarted) return
    isStarted = true
    isStopRequested = false
    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    isCrossWindowBlurEnabled = windowManager.isCrossWindowBlurEnabled
    applyBlurRadius()
    windowManager.addCrossWindowBlurEnabledListener(mainExecutor, blurEnabledListener)
  }

  fun setBlurRadius(radius: Float) {
    if (isStopRequested) return
    val clampedRadius = radius.coerceAtLeast(0f)
    if (abs(blurRadius - clampedRadius) < MIN_RADIUS_DELTA) return
    blurAnimator?.cancel()
    blurAnimator = null
    blurRadius = clampedRadius
    if (isStarted) {
      applyBlurRadius()
    }
  }

  fun animateBlurRadius(radius: Float, duration: Long) {
    if (!isStarted || isStopRequested) return
    animateBlurRadiusInternal(radius, duration)
  }

  fun finishWithAnimation(duration: Long) {
    if (!isStarted || isStopRequested) return

    if (isCrossWindowBlurEnabled) {
      stop()
      return
    }

    isStopRequested = true
    windowManager.removeCrossWindowBlurEnabledListener(blurEnabledListener)

    // Keep the fallback host blur alive after the dialog window or fragment view is detached.
    // Switching an enabled cross-window blur to this RenderEffect path causes a visible flash.
    HostViewBlurRegistry.update(hostView, this, blurRadius)
    updateWindowBlurRadius(0f)
    animateBlurRadiusInternal(0f, duration) {
      completeStop()
    }
  }

  fun stop() {
    if (!isStarted) return
    isStopRequested = true
    windowManager.removeCrossWindowBlurEnabledListener(blurEnabledListener)
    blurAnimator?.cancel()
    blurAnimator = null
    updateWindowBlurRadius(0f)
    completeStop()
  }

  private fun animateBlurRadiusInternal(radius: Float, duration: Long, onEnd: () -> Unit = {}) {
    val targetRadius = radius.coerceAtLeast(0f)
    blurAnimator?.cancel()
    if (abs(blurRadius - targetRadius) < MIN_RADIUS_DELTA || duration <= 0L) {
      blurRadius = targetRadius
      applyBlurRadius()
      onEnd()
      return
    }

    blurAnimator = ValueAnimator.ofFloat(blurRadius, targetRadius).apply {
      this.duration = duration
      interpolator = blurInterpolator
      addUpdateListener { animation ->
        blurRadius = animation.animatedValue as Float
        applyBlurRadius()
      }
      addListener(object : AnimatorListenerAdapter() {
        private var isCancelled = false

        override fun onAnimationCancel(animation: Animator) {
          isCancelled = true
        }

        override fun onAnimationEnd(animation: Animator) {
          if (!isCancelled) {
            blurAnimator = null
            onEnd()
          }
        }
      })
      start()
    }
  }

  private fun completeStop() {
    blurRadius = 0f
    isStarted = false
    isStopRequested = false
    HostViewBlurRegistry.remove(hostView, this)
  }

  private fun applyBlurRadius() {
    if (isCrossWindowBlurEnabled) {
      HostViewBlurRegistry.remove(hostView, this)
      updateWindowBlurRadius(blurRadius)
    } else {
      updateWindowBlurRadius(0f)
      HostViewBlurRegistry.update(hostView, this, blurRadius)
    }
  }

  private fun updateWindowBlurRadius(radius: Float) {
    val layoutParams = window.attributes
    val roundedRadius = radius.roundToInt()
    if (layoutParams.blurBehindRadius == roundedRadius) return
    layoutParams.blurBehindRadius = roundedRadius
    window.attributes = layoutParams
  }

  private object HostViewBlurRegistry {
    private val states = WeakHashMap<ViewGroup, HostBlurState>()

    fun update(hostView: ViewGroup?, owner: WindowBlurCompatController, radius: Float) {
      if (hostView == null) return
      val state = states.getOrPut(hostView) { HostBlurState() }
      if (radius > 0f) {
        state.requests[owner] = radius
      } else {
        state.requests.remove(owner)
      }
      scheduleLargestRadius(hostView, state)
    }

    fun remove(hostView: ViewGroup?, owner: WindowBlurCompatController) {
      if (hostView == null) return
      val state = states[hostView] ?: return
      state.requests.remove(owner)
      scheduleLargestRadius(hostView, state)
    }

    private fun scheduleLargestRadius(hostView: ViewGroup, state: HostBlurState) {
      if (state.isUpdateScheduled) return
      state.isUpdateScheduled = true
      hostView.postOnAnimation {
        val currentState = states[hostView] ?: return@postOnAnimation
        currentState.isUpdateScheduled = false
        applyLargestRadius(hostView, currentState)
      }
    }

    private fun applyLargestRadius(hostView: ViewGroup, state: HostBlurState) {
      val radius = state.requests.values.maxOrNull() ?: 0f
      if (radius <= 0f) {
        states.remove(hostView)
        state.blurEffect.clear(hostView)
        return
      }
      state.blurEffect.applyRadius(hostView, radius)
    }

    private class HostBlurState {
      val requests = mutableMapOf<WindowBlurCompatController, Float>()
      val blurEffect = HostViewBlurEffect()
      var isUpdateScheduled = false
    }

    private class HostViewBlurEffect {
      private var appliedRadius = Float.NaN
      private var overlay: SnapshotBlurOverlay? = null

      fun applyRadius(hostView: ViewGroup, radius: Float) {
        if (abs(appliedRadius - radius) < MIN_EFFECT_RADIUS_DELTA) return
        appliedRadius = radius
        val currentOverlay = overlay ?: SnapshotBlurOverlay.create(hostView)?.also {
          overlay = it
        } ?: return
        currentOverlay.applyRadius(radius)
      }

      fun clear(hostView: ViewGroup) {
        overlay?.clear(hostView)
        overlay = null
        hostView.setRenderEffect(null)
      }
    }

    private class SnapshotBlurOverlay(
      private val sharpLayer: ImageView,
      private val blurLayers: List<ImageView>
    ) {
      fun applyRadius(radius: Float) {
        sharpLayer.alpha = fixedSharpLayerAlpha(radius)
        val alphas = fixedBlurLayerAlphas(radius)
        blurLayers.forEachIndexed { index, layer ->
          layer.alpha = alphas[index]
        }
      }

      fun clear(hostView: ViewGroup) {
        (blurLayers + sharpLayer).forEach { layer ->
          layer.setImageDrawable(null)
          hostView.removeView(layer)
        }
      }

      companion object {
        fun create(hostView: ViewGroup): SnapshotBlurOverlay? {
          val hostWidth = hostView.width
          val hostHeight = hostView.height
          if (hostWidth <= 0 || hostHeight <= 0) return null

          val snapshot = try {
            Bitmap.createBitmap(hostWidth, hostHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
              hostView.draw(Canvas(bitmap))
              bitmap.prepareToDraw()
            }
          } catch (_: OutOfMemoryError) {
            return null
          }
          val blurBackgroundColor =
            hostView.context.getColorByAttr(android.R.attr.colorBackground)
          fun createLayer(radius: Float?): ImageView {
            return ImageView(hostView.context).apply {
              setImageBitmap(snapshot)
              scaleType = ImageView.ScaleType.FIT_XY
              isClickable = false
              isFocusable = false
              importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
              alpha = if (radius == null) 1f else 0f
              if (radius != null) {
                if (radius > FIXED_BLUR_RADII.first()) {
                  setBackgroundColor(blurBackgroundColor)
                }
                setRenderEffect(
                  RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
                )
              }
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
              hostView.addView(this)
            }
          }
          val sharpLayer = createLayer(null)
          val blurLayers = FIXED_BLUR_RADII.map(::createLayer)
          return SnapshotBlurOverlay(sharpLayer, blurLayers)
        }
      }
    }
  }

  private companion object {
    const val MIN_RADIUS_DELTA = 0.1f
    const val MIN_EFFECT_RADIUS_DELTA = 0.25f
  }
}

private val FIXED_BLUR_RADII = floatArrayOf(24f, 48f, 64f, 80f)

internal fun fixedSharpLayerAlpha(radius: Float): Float = (radius / FIXED_BLUR_RADII.first()).coerceIn(0f, 1f)

internal fun fixedBlurLayerAlphas(radius: Float): FloatArray {
  val clampedRadius = radius.coerceAtLeast(0f)
  val alphas = FloatArray(FIXED_BLUR_RADII.size)
  if (clampedRadius <= 0f) return alphas

  val exactIndex = FIXED_BLUR_RADII.indexOfFirst { it == clampedRadius }
  if (exactIndex >= 0) {
    alphas[exactIndex] = 1f
    return alphas
  }

  val upperIndex = FIXED_BLUR_RADII.indexOfFirst { it > clampedRadius }
  if (upperIndex < 0) {
    alphas[alphas.lastIndex] = 1f
    return alphas
  }

  val lowerRadius = FIXED_BLUR_RADII.getOrElse(upperIndex - 1) { 0f }
  if (upperIndex > 0) {
    alphas[upperIndex - 1] = 1f
  }
  alphas[upperIndex] =
    (clampedRadius - lowerRadius) / (FIXED_BLUR_RADII[upperIndex] - lowerRadius)
  return alphas
}
