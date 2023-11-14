package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup

class TimeNodeAddApkView(context: Context) : AViewGroup(context) {

  val name = AppCompatTextView(
    ContextThemeWrapper(
      context,
      R.style.TextView_SansSerifCondensedMedium
    )
  ).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    text = buildSpannedString {
      inSpans(ImageSpan(context, R.drawable.ic_archive)) {
        append(" ")
      }
      append(context.getString(R.string.album_item_comparison_choose_local_apk))
    }
  }

  init {
    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
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
    name.autoMeasure()
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + name.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    name.layout(name.toHorizontalCenter(this), name.toVerticalCenter(this))
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    return true
  }
}
