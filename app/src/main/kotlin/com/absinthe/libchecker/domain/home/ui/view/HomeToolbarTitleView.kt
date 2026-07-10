package com.absinthe.libchecker.domain.home.ui.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.home.model.HomeToolbarTitleState
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.ToolbarConnectionLoadingView
import kotlin.math.max

class HomeToolbarTitleView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : AViewGroup(context, attrs) {

  private val loadingGap = 6.dp

  private val titleView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setSingleLine()
    ellipsize = TextUtils.TruncateAt.END
    includeFontPadding = false
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleLarge))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
  }

  private val loadingView = ToolbarConnectionLoadingView(context).apply {
    layoutParams = LayoutParams(34.dp, 18.dp)
    alpha = 0f
    scaleX = 0.78f
    scaleY = 0.78f
    isGone = true
    contentDescription = context.getString(R.string.loading)
  }

  private var loadingRequested = false
  private var renderedState: HomeToolbarTitleState? = null
  private val visibilityInterpolator = FastOutSlowInInterpolator()

  init {
    addView(titleView)
    addView(loadingView)
  }

  fun bind(state: HomeToolbarTitleState) {
    val previousState = renderedState
    if (previousState?.title !== state.title) {
      titleView.text = state.title
      requestLayout()
    }
    if (previousState?.isLoading != state.isLoading) {
      renderLoading(state.isLoading)
    }
    renderedState = state
  }

  private fun renderLoading(loading: Boolean) {
    if (loadingRequested == loading) {
      return
    }
    loadingRequested = loading
    loadingView.animate().cancel()

    if (loading) {
      loadingView.isGone = false
      loadingView.start()
      requestLayout()
      loadingView.animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(180L)
        .setInterpolator(visibilityInterpolator)
        .start()
    } else {
      loadingView.animate()
        .alpha(0f)
        .scaleX(0.78f)
        .scaleY(0.78f)
        .setDuration(150L)
        .setInterpolator(visibilityInterpolator)
        .withEndAction {
          loadingView.stop()
          loadingView.isGone = true
          requestLayout()
        }
        .start()
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)
    val availableWidth = when (widthMode) {
      MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
      else -> MeasureSpec.getSize(widthMeasureSpec)
    }
    val availableHeight = when (heightMode) {
      MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
      else -> MeasureSpec.getSize(heightMeasureSpec)
    }

    val loadingVisible = !loadingView.isGone
    var loadingWidth = 0
    var loadingHeight = 0
    if (loadingVisible) {
      loadingView.measure(
        loadingView.layoutParams.width.toExactlyMeasureSpec(),
        loadingView.layoutParams.height.toExactlyMeasureSpec()
      )
      loadingWidth = loadingView.measuredWidth
      loadingHeight = loadingView.measuredHeight
    }

    val gap = if (loadingVisible) loadingGap else 0
    val titleMaxWidth = (availableWidth - paddingStart - paddingEnd - loadingWidth - gap)
      .coerceAtLeast(0)
    titleView.measure(
      titleMaxWidth.toAtMostMeasureSpec(),
      (availableHeight - paddingTop - paddingBottom).coerceAtLeast(0).toAtMostMeasureSpec()
    )

    val contentWidth = titleView.measuredWidth + gap + loadingWidth
    val contentHeight = max(titleView.measuredHeight, loadingHeight)
    val desiredWidth = paddingStart + paddingEnd + contentWidth
    val desiredHeight = paddingTop + paddingBottom + contentHeight

    setMeasuredDimension(
      resolveSize(desiredWidth, widthMeasureSpec),
      resolveSize(desiredHeight, heightMeasureSpec)
    )
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    val loadingVisible = !loadingView.isGone
    val gap = if (loadingVisible) loadingGap else 0
    val centerY = paddingTop + (height - paddingTop - paddingBottom) / 2
    val titleTop = centerY - titleView.measuredHeight / 2
    val loadingTop = centerY - loadingView.measuredHeight / 2
    val isRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL

    if (isRtl) {
      var x = paddingStart
      if (loadingVisible) {
        loadingView.layout(x, loadingTop, true)
        x += loadingView.measuredWidth + gap
      }
      titleView.layout(x, titleTop, true)
    } else {
      var x = paddingStart
      titleView.layout(x, titleTop)
      x += titleView.measuredWidth + gap
      if (loadingVisible) {
        loadingView.layout(x, loadingTop)
      }
    }
  }

  override fun onDetachedFromWindow() {
    loadingView.stop()
    super.onDetachedFromWindow()
  }
}
