package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class ComparisonDashboardView(context: Context) : MaterialCardView(context, null, R.style.AlbumMaterialCard) {

  val container = SnapshotDashboardContainerView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    val padding = context.getDimensionPixelSize(R.dimen.normal_padding)
    setPadding(padding, padding, padding, padding)
    clipToPadding = false
  }

  init {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    radius = 8.dp.toFloat()
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSecondaryContainer))
    addView(container)
  }

  class SnapshotDashboardContainerView(context: Context) : AViewGroup(context) {

    val leftPart = ComparisonDashboardHalfView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      horizontalGravity = Gravity.START
    }

    val rightPart = ComparisonDashboardHalfView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      horizontalGravity = Gravity.END
    }

    init {
      background = null
      addView(leftPart)
      addView(rightPart)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      val width = (measuredWidth - paddingStart - paddingEnd) / 2
      leftPart.measure(
        width.toExactlyMeasureSpec(),
        leftPart.defaultHeightMeasureSpec(this)
      )
      rightPart.measure(
        width.toExactlyMeasureSpec(),
        rightPart.defaultHeightMeasureSpec(this)
      )
      setMeasuredDimension(
        measuredWidth,
        paddingTop + leftPart.measuredHeight.coerceAtLeast(rightPart.measuredHeight) + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      leftPart.layout(paddingStart, paddingTop)
      rightPart.layout(paddingEnd, paddingTop, fromRight = true)
    }
  }
}
