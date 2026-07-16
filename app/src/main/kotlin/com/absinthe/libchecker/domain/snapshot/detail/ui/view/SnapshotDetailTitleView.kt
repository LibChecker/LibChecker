package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetailCountAdapter
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailTitleRenderState
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getDimensionByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class SnapshotDetailTitleView(context: Context) : MaterialCardView(context) {

  private val countAdapter = SnapshotDetailCountAdapter()

  private val container = SnapshotDetailTitleContainerView(context, countAdapter).apply {
    layoutParams = ViewGroup.LayoutParams(
      LayoutParams.MATCH_PARENT,
      context.getDimensionByAttr(android.R.attr.listPreferredItemHeightSmall).toInt()
    )
  }

  init {
    setSmoothRoundCorner(16.dp)
    strokeColor = Color.TRANSPARENT
    addView(container)
  }

  fun render(state: SnapshotDetailTitleRenderState) {
    contentDescription = state.contentDescription
    container.render(state)
    countAdapter.setList(state.counts)
    onExpansionToggled(state.expanded)
  }

  private fun onExpansionToggled(expanded: Boolean) {
    val start: Float
    val target: Float

    if (expanded) {
      start = 0f
      target = 90f
    } else {
      start = 90f
      target = 0f
    }

    ObjectAnimator.ofFloat(container.arrow, View.ROTATION, start, target).apply {
      duration = 200
      start()
    }
  }

  private class SnapshotDetailTitleContainerView(
    context: Context,
    countAdapter: SnapshotDetailCountAdapter
  ) : AViewGroup(context) {

    val arrow = ImageView(context).apply {
      layoutParams = LayoutParams(24.dp, 24.dp).also {
        it.marginStart = 16.dp
      }
      setImageResource(R.drawable.ic_arrow)
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }.also {
      addView(it)
    }

    val title = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(10.dp, 6.dp, 10.dp, 6.dp)
      text = context.getString(R.string.snapshot_empty_list_title)
      setTextAppearance(context.getResourceIdByAttr(android.R.attr.textAppearanceListItemSmall))
      setTypeface(null, Typeface.BOLD)
    }.also {
      addView(it)
    }

    val list = RecyclerView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      adapter = countAdapter
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }.also {
      addView(it)
    }

    fun render(state: SnapshotDetailTitleRenderState) {
      title.text = state.title
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      arrow.autoMeasure()
      title.autoMeasure()
      list.autoMeasure()
      setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      arrow.layout(arrow.marginStart, arrow.toVerticalCenter(this))
      title.layout(arrow.marginStart + arrow.measuredWidth, title.toVerticalCenter(this))
      list.layout(
        arrow.marginStart + arrow.measuredWidth + title.measuredWidth,
        list.toVerticalCenter(this)
      )
    }
  }
}
