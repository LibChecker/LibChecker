package com.absinthe.libchecker.view.detail

import android.content.Context
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.app.IHeaderView

class XmlBottomSheetView(context: Context) : AViewGroup(context), IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = "XML"
  }

  private val container = ScrollView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  val text = AppCompatTextView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    textSize = 10f
    setTextIsSelectable(true)
  }

  init {
    addView(header)
    addView(container)
    container.addView(text)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val maxWidth = measuredWidth - paddingStart - paddingEnd
    header.measure(maxWidth.toExactlyMeasureSpec(), defaultHeightMeasureSpec(this))
    container.measure(maxWidth.toExactlyMeasureSpec(), defaultHeightMeasureSpec(this))
    setMeasuredDimension(measuredWidth, header.measuredHeight + container.measuredHeight + paddingTop + paddingBottom)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    header.layout(paddingStart, paddingTop)
    container.layout(paddingStart, header.bottom)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
