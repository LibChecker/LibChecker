package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceAction
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceItemDisplay
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

  private val originatingView = AppInstallSourceItemView(
    context,
    context.getString(R.string.lib_detail_app_install_source_originating_package)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  private val installingView = AppInstallSourceItemView(
    context,
    context.getString(R.string.lib_detail_app_install_source_installing_package)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  private val installedTimeView = AppInstallTimeItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  private val dexoptView = AppDexoptItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  private val contentViews: List<View> = listOf(
    originatingView,
    installingView,
    installedTimeView,
    dexoptView
  )

  init {
    setPadding(24.dp, 16.dp, 24.dp, 16.dp)
    addView(header)
    contentViews.forEach { addView(it) }
  }

  fun bind(
    display: AppInstallSourceBottomSheetDisplay,
    onAction: (AppInstallSourceAction) -> Unit
  ) {
    bindAppItem(originatingView, display.originatingApp, onAction)
    bindAppItem(installingView, display.installingApp, onAction)

    installedTimeView.isVisible = display.installedTime != null
    display.installedTime?.let(installedTimeView::bind)

    dexoptView.isVisible = display.dexoptInfo != null
    display.dexoptInfo?.let(dexoptView::bind)
    requestLayout()
  }

  private fun bindAppItem(
    view: AppInstallSourceItemView,
    display: AppInstallSourceItemDisplay?,
    onAction: (AppInstallSourceAction) -> Unit
  ) {
    view.isVisible = display != null
    display?.let { view.bind(it, onAction) }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.autoMeasure()
    contentViews.forEach {
      it.measure(
        it.defaultWidthMeasureSpec(this),
        if (it.isGone) 0 else it.defaultHeightMeasureSpec(this)
      )
    }
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        header.measuredHeight +
        contentViews.sumOf { if (it.isGone) 0 else it.measuredHeight } +
        paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    header.layout(0, paddingTop)
    var childTop = header.bottom
    contentViews.forEach {
      it.layout(paddingStart, childTop)
      childTop = it.bottom
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = header
}
