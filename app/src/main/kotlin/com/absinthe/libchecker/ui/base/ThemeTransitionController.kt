package com.absinthe.libchecker.ui.base

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.lang.ref.WeakReference

object ThemeTransitionController {

  private var requestId = 0L
  private var pendingChange: PendingNightModeChange? = null
  private var pendingEnter: PendingEnter? = null
  private val frozenFrameTransition = FrozenFrameTransitionState()

  fun applyNightMode(
    activity: AppCompatActivity,
    nightMode: Int,
    onWindowHidden: () -> Unit = {}
  ) {
    requestId += 1
    val currentRequestId = requestId
    val decorView = activity.window.decorView
    val currentUiMode = activity.resources.configuration.uiMode
    val applicationUiMode = activity.application.resources.configuration.uiMode

    pendingChange = PendingNightModeChange(
      activity = WeakReference(activity),
      activityClassName = activity.javaClass.name,
      requestId = currentRequestId,
      nightMode = nightMode,
      expectsConfigurationChange = nightModeRequiresConfigurationChange(
        currentUiMode = currentUiMode,
        applicationUiMode = applicationUiMode,
        nightMode = nightMode
      ),
      onWindowHidden = onWindowHidden
    )

    animateOut(decorView) {
      completePendingChange(
        activity = activity,
        expectedRequestId = currentRequestId,
        canAnimateCurrentWindow = true
      )
    }
  }

  fun recreateWithTransition(
    activity: AppCompatActivity,
    onFrameFrozen: () -> Unit = {}
  ) {
    if (!frozenFrameTransition.begin()) {
      return
    }
    requestId += 1
    val currentRequestId = requestId
    val decorView = activity.window.decorView
    val decorRoot = decorView as? ViewGroup
    if (decorRoot == null || decorView.width <= 0 || decorView.height <= 0) {
      frozenFrameTransition.finish()
      return
    }
    val frozenFrameView = ImageView(activity).apply {
      isClickable = true
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
      scaleType = ImageView.ScaleType.FIT_XY
    }
    decorRoot.addView(
      frozenFrameView,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    )
    captureWindowFrame(activity) { bitmap ->
      if (activity.isFinishing || activity.isDestroyed || requestId != currentRequestId) {
        decorRoot.removeView(frozenFrameView)
        bitmap?.recycle()
        frozenFrameTransition.finish()
        return@captureWindowFrame
      }
      frozenFrameView.setImageBitmap(bitmap)
      frozenFrameTransition.onFrameReady(onFrameFrozen)
      animateOut(decorView) {
        if (!activity.isFinishing && !activity.isDestroyed) {
          pendingEnter = PendingEnter(
            activityClassName = activity.javaClass.name,
            requestId = currentRequestId
          )
          activity.recreate()
        } else {
          frozenFrameTransition.finish()
        }
      }
    }
  }

  fun onActivityDestroyed(activity: AppCompatActivity) {
    frozenFrameTransition.finish()
    completePendingChange(
      activity = activity,
      expectedRequestId = pendingChange?.requestId,
      canAnimateCurrentWindow = false
    )
  }

  fun animateEnterIfNeeded(activity: AppCompatActivity) {
    val pending = pendingEnter ?: return
    if (pending.activityClassName != activity.javaClass.name) {
      pendingEnter = null
      return
    }

    pendingEnter = null
    val decorView = activity.window.decorView
    decorView.animate().cancel()
    decorView.alpha = 0f
    decorView.post {
      animateEnter(decorView)
    }
  }

  private fun completePendingChange(
    activity: AppCompatActivity,
    expectedRequestId: Long?,
    canAnimateCurrentWindow: Boolean
  ) {
    val pending = pendingChange ?: return
    if (
      pending.activity.get() !== activity ||
      pending.requestId != expectedRequestId
    ) {
      return
    }
    pendingChange = null

    if (canAnimateCurrentWindow && !activity.isFinishing && !activity.isDestroyed) {
      pending.onWindowHidden()
    }
    if (pending.expectsConfigurationChange && !activity.isFinishing) {
      pendingEnter = PendingEnter(
        activityClassName = pending.activityClassName,
        requestId = pending.requestId
      )
    }

    val defaultModeChanged = AppCompatDelegate.getDefaultNightMode() != pending.nightMode
    if (defaultModeChanged) {
      AppCompatDelegate.setDefaultNightMode(pending.nightMode)
    } else if (
      pending.expectsConfigurationChange &&
      canAnimateCurrentWindow &&
      !activity.isFinishing &&
      !activity.isDestroyed
    ) {
      activity.delegate.applyDayNight()
    }

    if (
      !pending.expectsConfigurationChange &&
      canAnimateCurrentWindow &&
      !activity.isFinishing &&
      !activity.isDestroyed
    ) {
      animateEnter(activity.window.decorView)
    }
  }

  private fun animateEnter(view: View) {
    view.animate().cancel()
    view.animate()
      .alpha(1f)
      .setDuration(ENTER_DURATION_MS)
      .setInterpolator(TRANSITION_INTERPOLATOR)
      .start()
  }

  private fun animateOut(view: View, onHidden: () -> Unit) {
    view.animate().cancel()
    view.animate()
      .alpha(0f)
      .setDuration(EXIT_DURATION_MS)
      .setInterpolator(TRANSITION_INTERPOLATOR)
      .withEndAction(onHidden)
      .start()
  }

  private fun captureWindowFrame(
    activity: AppCompatActivity,
    onFrameReady: (Bitmap?) -> Unit
  ) {
    val decorView = activity.window.decorView
    val bitmap = runCatching {
      Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
    }.getOrNull()
    if (bitmap == null) {
      onFrameReady(null)
      return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      runCatching {
        PixelCopy.request(
          activity.window,
          bitmap,
          { result ->
            if (result == PixelCopy.SUCCESS) {
              onFrameReady(bitmap)
            } else {
              bitmap.recycle()
              onFrameReady(captureDecorFrame(decorView))
            }
          },
          Handler(Looper.getMainLooper())
        )
      }.onFailure {
        bitmap.recycle()
        onFrameReady(captureDecorFrame(decorView))
      }
    } else {
      bitmap.recycle()
      onFrameReady(captureDecorFrame(decorView))
    }
  }

  private fun captureDecorFrame(decorView: View): Bitmap? {
    return runCatching {
      Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888).also {
        decorView.draw(Canvas(it))
      }
    }.getOrNull()
  }

  private data class PendingNightModeChange(
    val activity: WeakReference<AppCompatActivity>,
    val activityClassName: String,
    val requestId: Long,
    val nightMode: Int,
    val expectsConfigurationChange: Boolean,
    val onWindowHidden: () -> Unit
  )

  private data class PendingEnter(
    val activityClassName: String,
    val requestId: Long
  )

  private const val EXIT_DURATION_MS = 160L
  private const val ENTER_DURATION_MS = 260L
  private val TRANSITION_INTERPOLATOR = PathInterpolator(0.2f, 0f, 0f, 1f)
}

internal class FrozenFrameTransitionState {

  private var phase = Phase.IDLE

  fun begin(): Boolean {
    if (phase != Phase.IDLE) {
      return false
    }
    phase = Phase.CAPTURING
    return true
  }

  fun onFrameReady(applyChange: () -> Unit) {
    if (phase != Phase.CAPTURING) {
      return
    }
    phase = Phase.ANIMATING
    applyChange()
  }

  fun finish() {
    phase = Phase.IDLE
  }

  private enum class Phase {
    IDLE,
    CAPTURING,
    ANIMATING
  }
}

internal fun nightModeRequiresConfigurationChange(
  currentUiMode: Int,
  applicationUiMode: Int,
  nightMode: Int
): Boolean {
  val currentNightMode = currentUiMode and Configuration.UI_MODE_NIGHT_MASK
  val targetNightMode = when (nightMode) {
    AppCompatDelegate.MODE_NIGHT_NO -> Configuration.UI_MODE_NIGHT_NO
    AppCompatDelegate.MODE_NIGHT_YES -> Configuration.UI_MODE_NIGHT_YES
    else -> applicationUiMode and Configuration.UI_MODE_NIGHT_MASK
  }
  return currentNightMode != targetNightMode
}
