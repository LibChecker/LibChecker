package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.isGone
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AppInstallSourceBottomSheetView(context: Context) :
  AViewGroup(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.lib_detail_app_install_source_title)
  }

  val originatingView = AppInstallSourceItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    titleView.text = context.getString(R.string.lib_detail_app_install_source_originating_package)
  }

  val installingView = AppInstallSourceItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    titleView.text = context.getString(R.string.lib_detail_app_install_source_installing_package)
  }

  init {
    setPadding(24.dp, 16.dp, 24.dp, 16.dp)
    addView(header)
    addView(originatingView)
    addView(installingView)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.autoMeasure()
    originatingView.let {
      it.measure(
        it.defaultWidthMeasureSpec(this),
        if (it.isGone) 0 else it.defaultHeightMeasureSpec(this)
      )
    }
    installingView.let {
      it.measure(
        it.defaultWidthMeasureSpec(this),
        if (it.isGone) 0 else it.defaultHeightMeasureSpec(this)
      )
    }
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        header.measuredHeight +
        originatingView.measuredHeight +
        installingView.measuredHeight +
        paddingBottom
    )
  }

  override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
    header.layout(0, paddingTop)
    originatingView.layout(paddingStart, header.bottom)
    installingView.layout(paddingStart, originatingView.bottom)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
