package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.domain.snapshot.timenode.ui.adapter.TimeNodeItemAdapter
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.AViewGroup

class TimeNodeItemView(context: Context) : AViewGroup(context) {

  private val defaultNameColor: Int

  private val name = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
  }

  private val summary = AppCompatTextView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextColor(context.getColorByAttr(android.R.attr.textColorSecondary))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
  }

  private val adapter = TimeNodeItemAdapter()

  private val moreIndicator = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      gravity = Gravity.CENTER_VERTICAL
    }
    // noinspection AndroidLintSetTextI18n
    text = "…"
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
  }

  private val rvList = RecyclerView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    overScrollMode = OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    adapter = this@TimeNodeItemView.adapter
  }

  init {
    defaultNameColor = name.currentTextColor
    setPadding(10.dp, 6.dp, 10.dp, 6.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
    addView(name)
    addView(summary)
    addView(rvList)
  }

  fun bind(
    item: SnapshotTimeNodeItem,
    packageIconSources: Map<String, SnapshotPackageIconSource>
  ) {
    name.text = item.timestampText
    val appCountText = resources.getQuantityString(
      R.plurals.snapshot_time_node_apps_count,
      item.appCount,
      item.appCount
    )
    summary.text = if (item.isCurrent) {
      context.getString(
        R.string.snapshot_time_node_current_apps,
        context.getString(R.string.snapshot_time_node_current),
        appCountText
      )
    } else {
      appCountText
    }
    val primaryColor = context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
    name.setTextColor(if (item.isCurrent) primaryColor else defaultNameColor)
    summary.setTextColor(
      if (item.isCurrent) {
        primaryColor
      } else {
        context.getColorByAttr(android.R.attr.textColorSecondary)
      }
    )
    contentDescription = listOf(item.description, summary.text)
      .map(CharSequence::toString)
      .filter(String::isNotBlank)
      .joinToString()
    adapter.bind(item.topAppPackageNames.take(MAX_VISIBLE_APP_COUNT), packageIconSources)
    rvList.isVisible = item.topAppPackageNames.isNotEmpty()
    if (item.topAppPackageNames.size <= MAX_VISIBLE_APP_COUNT) {
      adapter.removeAllFooterView()
    } else {
      adapter.setFooterView(moreIndicator)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    summary.autoMeasure()
    if (rvList.isVisible) {
      rvList.autoMeasure()
    } else {
      rvList.measure(0.toExactlyMeasureSpec(), 0.toExactlyMeasureSpec())
    }
    name.measure(
      (measuredWidth - paddingStart - paddingEnd - summary.measuredWidth - TITLE_GAP.dp)
        .coerceAtLeast(0)
        .toExactlyMeasureSpec(),
      name.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      maxOf(
        MIN_HEIGHT.dp,
        paddingTop + paddingBottom + name.measuredHeight + rvList.measuredHeight
      )
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    name.layout(paddingStart, paddingTop)
    summary.layout(paddingEnd, summary.toViewVerticalCenter(name), fromRight = true)
    if (rvList.isVisible) {
      rvList.layout(paddingStart, name.bottom)
    }
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    return true
  }

  private companion object {
    const val MAX_VISIBLE_APP_COUNT = 6
    const val MIN_HEIGHT = 44
    const val TITLE_GAP = 8
  }
}
