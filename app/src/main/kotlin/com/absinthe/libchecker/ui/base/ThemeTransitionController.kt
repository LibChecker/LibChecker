package com.absinthe.libchecker.ui.base

import android.content.res.Configuration
import android.view.View
import android.view.animation.PathInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.lang.ref.WeakReference

object ThemeTransitionController {

  private var requestId = 0L
  private var pendingChange: PendingNightModeChange? = null
  private var pendingEnter: PendingEnter? = null

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
    onWindowHidden: () -> Unit = {}
  ) {
    requestId += 1
    val currentRequestId = requestId
    val decorView = activity.window.decorView
    animateOut(decorView) {
      if (!activity.isFinishing && !activity.isDestroyed) {
        onWindowHidden()
        pendingEnter = PendingEnter(
          activityClassName = activity.javaClass.name,
          requestId = currentRequestId
        )
        activity.recreate()
      }
    }
  }

  fun onActivityDestroyed(activity: AppCompatActivity) {
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
