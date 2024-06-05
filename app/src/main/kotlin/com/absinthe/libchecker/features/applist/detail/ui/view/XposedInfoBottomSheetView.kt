package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.detail.ui.adapter.XposedDetailItemAdapter
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class XposedInfoBottomSheetView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.xposed_module)
  }

  val contentAdapter = XposedDetailItemAdapter()

  val setting = AppInfoItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setIcon(R.drawable.ic_settings)
    setIconBackgroundTintColor(R.color.material_blue_grey_300)
    setIconTintColor(Color.WHITE)
  }

  private val xposedDetailContentView = BottomSheetRecyclerView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    )
    adapter = contentAdapter
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = false
    addItemDecoration(VerticalSpacesItemDecoration(4.dp))
  }

  init {
    orientation = VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    addView(header)
    addView(setting)
    addView(xposedDetailContentView)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  class XposedDetailItemView(context: Context) : AViewGroup(context) {

    val icon = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(24.dp, 24.dp)
    }

    val tip = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      alpha = 0.65f
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
    }

    val text = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
        it.topMargin = 0
      }
    }

    init {
      setPadding(8.dp, 8.dp, 8.dp, 8.dp)
      setBackgroundResource(R.drawable.bg_lib_detail_item)
      addView(icon)
      addView(tip)
      addView(text)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      val textWidth = measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - tip.marginStart
      if (tip.measuredWidth > textWidth) {
        tip.measure(textWidth.toExactlyMeasureSpec(), tip.defaultHeightMeasureSpec(this))
      }
      if (text.measuredWidth > textWidth) {
        text.measure(textWidth.toExactlyMeasureSpec(), text.defaultHeightMeasureSpec(this))
      }
      setMeasuredDimension(
        measuredWidth,
        (tip.measuredHeight + text.marginTop + text.measuredHeight).coerceAtLeast(icon.measuredHeight) + paddingTop + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      tip.layout(paddingStart + icon.measuredWidth + tip.marginStart, paddingTop)
      text.layout(paddingStart + icon.measuredWidth + tip.marginStart, tip.bottom + text.marginTop)
    }
  }
}
