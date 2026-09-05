package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.FrameLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceLoadingState
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.app.RingDotsView
import com.google.android.material.progressindicator.CircularProgressIndicator

class LibReferenceLoadingView(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

  init {
    clipChildren = false
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    contentDescription = context.getString(R.string.loading)
  }

  val loadingView = RingDotsView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.general_loading_size)
    layoutParams = LayoutParams(size, size).also {
      it.gravity = Gravity.CENTER
    }
    addView(this)
  }

  private val progressIndicator = CircularProgressIndicator(
    ContextThemeWrapper(context, R.style.App_Widget_M3E_CircularProgressIndicator)
  ).apply {
    isIndeterminate = true
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
      it.gravity = Gravity.CENTER
    }
    addView(this)
  }

  private var wasDeterminate = false

  fun bind(state: LibReferenceLoadingState) {
    val progress = state.overallProgress
    if (progress != null) {
      progressIndicator.setProgressCompat(progress, true)
    } else if (wasDeterminate || !progressIndicator.isIndeterminate) {
      // A new request may arrive during the pending transition to determinate mode.
      progressIndicator.isIndeterminate = false
      progressIndicator.setProgressCompat(0, false)
      progressIndicator.isIndeterminate = true
    }
    wasDeterminate = progress != null
  }
}
