package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.inSpans
import androidx.core.view.children
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailCountRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailTitleRenderState
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.R as MaterialR

class SnapshotDetailTitleView(context: Context) : AViewGroup(context) {

  private val title = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceTitleMedium))
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    setTypeface(typeface, Typeface.BOLD)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val counts = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceLabelLarge))
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val arrow = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp)
    setImageResource(R.drawable.ic_arrow)
    imageTintList = android.content.res.ColorStateList.valueOf(
      context.getColorByAttr(MaterialR.attr.colorOnSurfaceVariant)
    )
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val divider = View(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dp)
    setBackgroundColor(context.getColorByAttr(MaterialR.attr.colorOutlineVariant))
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private var countsOnSecondLine = false
  private var renderedExpanded: Boolean? = null

  init {
    minimumHeight = 52.dp
    setPadding(12.dp, 10.dp, 12.dp, 10.dp)
    setBackgroundColor(context.getColorByAttr(MaterialR.attr.colorSurface))
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    isFocusable = true
    foreground = context.getDrawable(
      context.getResourceIdByAttr(android.R.attr.selectableItemBackground)
    )
  }

  fun render(state: SnapshotDetailTitleRenderState) {
    contentDescription = state.contentDescription
    title.text = state.title
    counts.apply {
      text = buildSnapshotDetailCountText(context, state.counts)
      isVisible = state.counts.isNotEmpty()
    }
    renderExpansion(state.expanded)
  }

  private fun renderExpansion(expanded: Boolean) {
    val targetRotation = if (expanded) 90f else 0f
    val previous = renderedExpanded
    renderedExpanded = expanded
    if (previous == null) {
      arrow.rotation = targetRotation
      return
    }
    if (previous == expanded) {
      return
    }
    ObjectAnimator.ofFloat(arrow, View.ROTATION, arrow.rotation, targetRotation).apply {
      duration = 180
      start()
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.filter { it !== divider }.forEach { it.autoMeasure() }
    divider.measure(measuredWidth.toExactlyMeasureSpec(), 1.dp.toExactlyMeasureSpec())

    val availableWidth = measuredWidth - paddingStart - paddingEnd - arrow.measuredWidth - 8.dp
    if (title.measuredWidth > availableWidth) {
      title.measure(
        availableWidth.toExactlyMeasureSpec(),
        title.defaultHeightMeasureSpec(this)
      )
    }
    countsOnSecondLine = counts.isVisible &&
      title.measuredWidth + 12.dp + counts.measuredWidth > availableWidth
    if (counts.isVisible && counts.measuredWidth > availableWidth) {
      counts.measure(
        availableWidth.toExactlyMeasureSpec(),
        counts.defaultHeightMeasureSpec(this)
      )
    }
    val contentHeight = if (countsOnSecondLine) {
      title.measuredHeight + 2.dp + counts.measuredHeight
    } else {
      maxOf(title.measuredHeight, counts.measuredHeight, arrow.measuredHeight)
    }
    setMeasuredDimension(
      measuredWidth,
      (paddingTop + contentHeight + paddingBottom).coerceAtLeast(minimumHeight)
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val contentTop = paddingTop
    val arrowLeft = measuredWidth - paddingEnd - arrow.measuredWidth
    arrow.layout(arrowLeft, (measuredHeight - arrow.measuredHeight) / 2)
    title.layout(paddingStart, contentTop)
    if (counts.isVisible) {
      if (countsOnSecondLine) {
        counts.layout(paddingStart, title.bottom + 2.dp)
      } else {
        counts.layout(
          arrowLeft - 8.dp - counts.measuredWidth,
          (measuredHeight - counts.measuredHeight) / 2
        )
      }
    }
    divider.layout(0, measuredHeight - divider.measuredHeight)
  }
}

internal fun buildSnapshotDetailCountText(
  context: Context,
  counts: List<SnapshotDetailCountRenderState>
): CharSequence {
  return SpannableStringBuilder().apply {
    counts.forEachIndexed { index, count ->
      if (index > 0) {
        append("   ")
      }
      inSpans(
        ForegroundColorSpan(count.colorRes.getColor(context)),
        StyleSpan(Typeface.BOLD)
      ) {
        append(count.text)
      }
    }
  }
}
