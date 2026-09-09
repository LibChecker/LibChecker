package com.absinthe.libchecker.view.app

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.google.android.material.progressindicator.CircularProgressIndicator

open class DotLoadingView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
  val loadingView = RingDotsView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.general_loading_size)
    layoutParams = LayoutParams(size, size, Gravity.CENTER)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  private val progressIndicator = CircularProgressIndicator(
    ContextThemeWrapper(context, R.style.App_Widget_M3E_CircularProgressIndicator)
  ).apply {
    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
    visibility = INVISIBLE
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  private var hasProgress = false
  private var wasDeterminate = false
  private var active = false
  private var lifecycle: Lifecycle? = null
  private val lifecycleObserver = LifecycleEventObserver { _, _ -> updateActiveState() }

  init {
    clipChildren = false
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    contentDescription = context.getString(R.string.loading)
    addView(loadingView)
    addView(progressIndicator)
  }

  fun setProgress(progress: Int?, animated: Boolean = true) {
    hasProgress = true
    if (progress != null) {
      progressIndicator.setProgressCompat(progress.coerceIn(0, 100), animated)
    } else if (wasDeterminate || !progressIndicator.isIndeterminate) {
      // Cancel a pending transition before starting a new indeterminate request.
      progressIndicator.isIndeterminate = false
      progressIndicator.setProgressCompat(0, false)
      progressIndicator.isIndeterminate = true
    }
    wasDeterminate = progress != null
    progressIndicator.visibility = if (active) VISIBLE else INVISIBLE
  }

  fun setActive(active: Boolean) {
    this.active = active
    progressIndicator.visibility = if (active && hasProgress) VISIBLE else INVISIBLE
    if (active) loadingView.start() else loadingView.stop()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    lifecycle = findViewTreeLifecycleOwner()?.lifecycle
    lifecycle?.addObserver(lifecycleObserver)
    updateActiveState()
  }

  override fun onVisibilityAggregated(isVisible: Boolean) {
    super.onVisibilityAggregated(isVisible)
    updateActiveState()
  }

  override fun onDetachedFromWindow() {
    lifecycle?.removeObserver(lifecycleObserver)
    lifecycle = null
    setActive(false)
    super.onDetachedFromWindow()
  }

  private fun updateActiveState() {
    setActive(
      isAttachedToWindow && isShown && windowVisibility == VISIBLE &&
        (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true)
    )
  }
}
