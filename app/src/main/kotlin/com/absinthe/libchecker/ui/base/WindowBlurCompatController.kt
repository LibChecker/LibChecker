package com.absinthe.libchecker.ui.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlendMode
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.RequiresApi
import com.absinthe.libchecker.utils.extensions.activity
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
        hostView.setRenderEffect(null)
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
      private val identityEffect = RenderEffect.createOffsetEffect(0f, 0f)
      private val blurEffects = FALLBACK_BLUR_RADII.associateWith { radius ->
        RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
      }
      private var appliedRadius = Float.NaN

      fun applyRadius(hostView: View, radius: Float) {
        if (abs(appliedRadius - radius) < MIN_EFFECT_RADIUS_DELTA) return
        appliedRadius = radius

        val upperIndex = FALLBACK_BLUR_RADII.indexOfFirst { radius < it }
        if (upperIndex == -1) {
          hostView.setRenderEffect(blurEffects.getValue(FALLBACK_BLUR_RADII.last()))
          return
        }

        val upperRadius = FALLBACK_BLUR_RADII[upperIndex]
        val lowerRadius = FALLBACK_BLUR_RADII.getOrNull(upperIndex - 1) ?: 0f
        val lowerEffect = blurEffects[lowerRadius] ?: identityEffect
        val upperEffect = blurEffects.getValue(upperRadius)
        val mix = ((radius - lowerRadius) / (upperRadius - lowerRadius)).coerceIn(0f, 1f)

        if (mix <= MIN_EFFECT_MIX_DELTA) {
          hostView.setRenderEffect(lowerEffect)
          return
        }
        if (mix >= 1f - MIN_EFFECT_MIX_DELTA) {
          hostView.setRenderEffect(upperEffect)
          return
        }

        val alphaMatrix = ColorMatrix().apply {
          setScale(1f, 1f, 1f, mix)
        }
        val translucentUpperEffect = RenderEffect.createColorFilterEffect(
          ColorMatrixColorFilter(alphaMatrix),
          upperEffect
        )
        hostView.setRenderEffect(
          RenderEffect.createBlendModeEffect(
            lowerEffect,
            translucentUpperEffect,
            BlendMode.SRC_OVER
          )
        )
      }
    }
  }

  private companion object {
    const val MIN_RADIUS_DELTA = 0.1f
    const val MIN_EFFECT_RADIUS_DELTA = 0.25f
    const val MIN_EFFECT_MIX_DELTA = 0.01f
    val FALLBACK_BLUR_RADII = listOf(8f, 20f, 40f, 60f, 80f)
  }
}
