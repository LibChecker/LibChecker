package com.absinthe.libchecker.domain.snapshot.comparison.ui.view

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardAction
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardState
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonSide
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.divider.MaterialDivider

class ComparisonDashboardView(context: Context) : MaterialCardView(context, null, R.style.AlbumMaterialCard) {

  private val container = ComparisonDashboardContainerView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    val padding = context.getDimensionPixelSize(R.dimen.normal_padding)
    setPadding(padding, padding, padding, padding)
    clipToPadding = false
  }

  init {
    layoutParams = ViewGroup.LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    setSmoothRoundCorner(12.dp)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh))
    addView(container)
  }

  internal fun bind(
    state: ComparisonDashboardState,
    onAction: (ComparisonDashboardAction) -> Unit
  ) {
    container.bind(state, onAction)
  }

  private class ComparisonDashboardContainerView(context: Context) : AViewGroup(context) {

    private val leftPart = ComparisonDashboardHalfView(context, Gravity.START).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    private val rightPart = ComparisonDashboardHalfView(context, Gravity.END).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    private val divider = MaterialDivider(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    }

    init {
      background = null
      addView(leftPart)
      addView(rightPart)
      addView(divider)
    }

    fun bind(
      state: ComparisonDashboardState,
      onAction: (ComparisonDashboardAction) -> Unit
    ) {
      leftPart.bind(state.left)
      rightPart.bind(state.right)
      leftPart.setOnClickListener {
        if (!AntiShakeUtils.isInvalidClick(it)) {
          onAction(ComparisonDashboardAction.SelectSide(SnapshotComparisonSide.LEFT))
        }
      }
      rightPart.setOnClickListener {
        if (!AntiShakeUtils.isInvalidClick(it)) {
          onAction(ComparisonDashboardAction.SelectSide(SnapshotComparisonSide.RIGHT))
        }
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      val dividerWidth = 1.dp
      val width = (measuredWidth - paddingStart - paddingEnd - dividerWidth) / 2

      leftPart.measure(
        width.toExactlyMeasureSpec(),
        leftPart.defaultHeightMeasureSpec(this)
      )
      rightPart.measure(
        width.toExactlyMeasureSpec(),
        rightPart.defaultHeightMeasureSpec(this)
      )
      divider.measure(
        dividerWidth.toExactlyMeasureSpec(),
        divider.defaultHeightMeasureSpec(this)
      )
      setMeasuredDimension(
        measuredWidth,
        paddingTop + leftPart.measuredHeight.coerceAtLeast(rightPart.measuredHeight) + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      leftPart.layout(paddingStart, paddingTop)
      divider.layout(leftPart.right, paddingTop)
      rightPart.layout(paddingEnd, paddingTop, fromRight = true)
    }
  }
}
