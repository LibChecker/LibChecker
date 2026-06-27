package com.absinthe.libchecker.domain.snapshot.track.ui.view

import android.content.Context
import android.graphics.drawable.Drawable
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.RingDotsView

class TrackLoadingView(
  context: Context,
  private val loadRandomAppIcon: suspend () -> Drawable?
) : AViewGroup(context) {

  private val loading = RingDotsView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.general_loading_size)
    layoutParams = LayoutParams(size, size)
  }

  init {
    addView(loading)
    loading.setAppIconHighlightProvider(loadRandomAppIcon)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    loading.autoMeasure()
    setMeasuredDimension(measuredWidth, measuredHeight)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    loading.layout(loading.toHorizontalCenter(this), loading.toVerticalCenter(this))
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    loading.start()
  }

  override fun onDetachedFromWindow() {
    loading.stop()
    super.onDetachedFromWindow()
  }
}
