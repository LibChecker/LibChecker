package com.absinthe.libchecker.utils.extensions

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun View.applySystemBarsPadding(
  left: Boolean = false,
  top: Boolean = false,
  right: Boolean = false,
  bottom: Boolean = false
) {
  val initialPadding = Rect(paddingLeft, paddingTop, paddingRight, paddingBottom)
  ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
    view.updatePadding(
      left = initialPadding.left + if (left) insets.left else 0,
      top = initialPadding.top + if (top) insets.top else 0,
      right = initialPadding.right + if (right) insets.right else 0,
      bottom = initialPadding.bottom + if (bottom) insets.bottom else 0
    )
    windowInsets
  }
  requestApplyInsetsWhenAttached()
}

fun View.applySystemBarsMargin(
  left: Boolean = false,
  top: Boolean = false,
  right: Boolean = false,
  bottom: Boolean = false
) {
  val initialMargins = (layoutParams as? ViewGroup.MarginLayoutParams)?.let {
    Rect(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin)
  } ?: Rect()

  ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
    val marginLayoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams
      ?: return@setOnApplyWindowInsetsListener windowInsets
    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
    marginLayoutParams.setMargins(
      initialMargins.left + if (left) insets.left else 0,
      initialMargins.top + if (top) insets.top else 0,
      initialMargins.right + if (right) insets.right else 0,
      initialMargins.bottom + if (bottom) insets.bottom else 0
    )
    view.layoutParams = marginLayoutParams
    windowInsets
  }
  requestApplyInsetsWhenAttached()
}

private fun View.requestApplyInsetsWhenAttached() {
  if (isAttachedToWindow) {
    ViewCompat.requestApplyInsets(this)
  } else {
    addOnAttachStateChangeListener(
      object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
          view.removeOnAttachStateChangeListener(this)
          ViewCompat.requestApplyInsets(view)
        }

        override fun onViewDetachedFromWindow(view: View) = Unit
      }
    )
  }
}
