package com.absinthe.libchecker.features.applist.detail.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.features.applist.detail.ui.adapter.LibStringAdapter

class ProcessIndicatorView(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

  var isExpand: Boolean = false
    private set
  var animationDuration = 350L

  private val libStringAdapter = LibStringAdapter("", ACTIVITY)
  private var measuredViewHeight: Int = 0

  private val indicators = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    this.adapter = libStringAdapter
  }

  init {
    addView(indicators)
    post {
      measuredViewHeight = measuredHeight
      setViewHeight(0)
    }
  }

  private fun animateToggle(animationDuration: Long) {
    if (isExpand) {
      ValueAnimator.ofFloat(0f, measuredViewHeight.toFloat())
    } else {
      ValueAnimator.ofFloat(measuredViewHeight.toFloat(), 0f)
    }.also {
      it.duration = animationDuration / 2
      it.startDelay = animationDuration / 2
      it.addUpdateListener { animation ->
        setViewHeight((animation.animatedValue as Float).toInt())
      }
    }.start()
  }

  private fun setViewHeight(height: Int) {
    (layoutParams as ViewGroup.LayoutParams).height = height
    requestLayout()
  }

  fun collapse() {
    isExpand = false
    animateToggle(animationDuration)
  }

  fun expand() {
    isExpand = true
    animateToggle(animationDuration)
  }

  fun toggleExpand() {
    if (isExpand) {
      collapse()
    } else {
      expand()
    }
  }
}
