package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup
import kotlin.math.max

class TimeNodeAddApkView(context: Context) : AViewGroup(context) {

  private val iconGap = 6.dp

  private val icon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(22.dp, 22.dp)
    setImageResource(R.drawable.ic_archive)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  private val name = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    includeFontPadding = false
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    text = context.getString(R.string.album_item_comparison_choose_local_apk)
  }

  init {
    setPadding(10.dp, 10.dp, 10.dp, 10.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
    contentDescription = context.getString(R.string.album_item_comparison_choose_local_apk)
    addView(icon)
    addView(name)
  }

  override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
    return ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    icon.autoMeasure()
    val availableNameWidth = (measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - iconGap)
      .coerceAtLeast(0)
    name.measure(
      availableNameWidth.toAtMostMeasureSpec(),
      name.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      resolveSize(paddingTop + paddingBottom + max(icon.measuredHeight, name.measuredHeight), heightMeasureSpec)
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val contentWidth = icon.measuredWidth + iconGap + name.measuredWidth
    val startX = paddingStart + ((measuredWidth - paddingStart - paddingEnd - contentWidth) / 2).coerceAtLeast(0)
    icon.layout(startX, icon.toVerticalCenter(this))
    name.layout(startX + icon.measuredWidth + iconGap, name.toVerticalCenter(this))
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    return true
  }
}
