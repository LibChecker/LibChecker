package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AppInfoBottomSheetView(context: Context) :
  AViewGroup(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.further_operation)
  }

  val launch = AppInfoItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    setIcon(R.drawable.ic_launch)
    setIconBackgroundTintColor(R.color.material_deep_purple_300)
    setIconTintColor(Color.WHITE)
    setText(R.string.app_info_launch)
  }

  val setting = AppInfoItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setIcon(R.drawable.ic_settings)
    setIconBackgroundTintColor(R.color.material_blue_grey_300)
    setIconTintColor(Color.WHITE)
    setText(R.string.app_info_settings)
  }

  val list = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
  }

  init {
    setPadding(
      24.dp,
      16.dp,
      24.dp,
      (16.dp - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    addView(header)
    addView(launch)
    addView(setting)
    addView(list)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.autoMeasure()
    val itemWidth = (measuredWidth - paddingStart - paddingEnd) / 4
    launch.measure(itemWidth.toExactlyMeasureSpec(), launch.defaultHeightMeasureSpec(this))
    setting.measure(itemWidth.toExactlyMeasureSpec(), setting.defaultHeightMeasureSpec(this))
    list.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      list.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop + header.measuredHeight + launch.marginTop + launch.measuredHeight + list.measuredHeight + paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    header.layout(0, paddingTop)
    launch.layout(paddingStart, header.bottom + launch.marginTop)
    setting.layout(paddingStart + launch.measuredWidth, launch.top)
    list.layout(paddingStart, launch.bottom)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
