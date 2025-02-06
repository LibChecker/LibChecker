package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.snapshot.ui.adapter.TimeNodeItemAdapter
import com.absinthe.libchecker.view.AViewGroup

class TimeNodeItemView(context: Context) : AViewGroup(context) {

  val name = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
  }

  val adapter = TimeNodeItemAdapter().apply {
    setEmptyView(
      AppCompatTextView(
        ContextThemeWrapper(
          context,
          R.style.TextView_SansSerifCondensedMedium
        )
      ).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 24.dp)
        gravity = Gravity.CENTER
        text = context.getString(R.string.album_snapshot_top_apps_not_initialized)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      }
    )
  }

  private val rvList = RecyclerView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    overScrollMode = OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    adapter = this@TimeNodeItemView.adapter
  }

  init {
    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
    addView(name)
    addView(rvList)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    rvList.autoMeasure()
    name.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      name.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + name.measuredHeight + rvList.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    name.layout(paddingStart, paddingTop)
    rvList.layout(paddingStart, name.bottom)
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    return true
  }
}
