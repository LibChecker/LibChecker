package com.absinthe.libchecker.view.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.animation.doOnEnd
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import timber.log.Timber

class HeightAnimatableLinearLayout :
  LinearLayout,
  View.OnLayoutChangeListener {
  var animationDuration = 350L
  private var animator: ValueAnimator = ObjectAnimator()

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  )

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    addOnLayoutChangeListener(this)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    animator.cancel()
    removeOnLayoutChangeListener(this)
  }

  override fun onLayoutChange(
    v: View?,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    oldLeft: Int,
    oldTop: Int,
    oldRight: Int,
    oldBottom: Int
  ) {
    if ((bottom - top) != (oldBottom - oldTop)) {
      enqueueAnimation {
        animateHeight(from = oldBottom - oldTop, to = bottom - top, onEnd = { })
      }
    }
  }

  private fun animateHeight(from: Int, to: Int, onEnd: () -> Unit) {
    animator.cancel()
    animator = ObjectAnimator.ofFloat(0f, 1f).apply {
      duration = animationDuration
      interpolator = FastOutSlowInInterpolator()
      Timber.d("animateHeight: $from -> $to")

      addUpdateListener {
        val scale = it.animatedValue as Float
        val newHeight = ((to - from) * scale + from).toInt()
        setClippedHeight(newHeight)
      }
      doOnEnd { onEnd() }
      start()
    }
  }

  private fun enqueueAnimation(action: () -> Unit) {
    if (!animator.isRunning) {
      action()
    } else {
      animator.doOnEnd { action() }
    }
  }

  private fun setClippedHeight(newHeight: Int) {
    layoutParams.height = newHeight
    invalidate()
  }
}
