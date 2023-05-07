package com.absinthe.libchecker.view.detail

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libchecker.view.applist.AppItemView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AppInstallSourceBottomSheetView(context: Context) : AViewGroup(context), IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.lib_detail_app_install_source_title)
  }

  val initiatingTitleView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    text = context.getString(R.string.lib_detail_app_install_source_initiating_package)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  val initiatingPackageView = AppItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    strokeWidth = 1.dp
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutline)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSecondaryContainer))
  }

  val installingTitleView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    text = context.getString(R.string.lib_detail_app_install_source_installing_package)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  val installingPackageView = AppItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    strokeWidth = 1.dp
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutline)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSecondaryContainer))
  }

  init {
    setPadding(24.dp, 16.dp, 24.dp, 16.dp)
    addView(header)
    addView(initiatingTitleView)
    addView(initiatingPackageView)
    addView(installingTitleView)
    addView(installingPackageView)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.autoMeasure()
    initiatingTitleView.autoMeasure()
    installingTitleView.autoMeasure()
    initiatingPackageView.let {
      it.measure(
        (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
        if (it.isGone) 0 else it.defaultHeightMeasureSpec(this)
      )
    }
    installingPackageView.let {
      it.measure(
        (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
        if (it.isGone) 0 else it.defaultHeightMeasureSpec(this)
      )
    }
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        header.measuredHeight +
        initiatingTitleView.marginTop +
        initiatingTitleView.measuredHeight +
        initiatingPackageView.marginTop +
        initiatingPackageView.measuredHeight +
        installingTitleView.marginTop +
        installingTitleView.measuredHeight +
        installingPackageView.marginTop +
        installingPackageView.measuredHeight +
        paddingBottom
    )
  }

  override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
    header.layout(0, paddingTop)
    initiatingTitleView.layout(paddingStart, header.bottom + initiatingTitleView.marginTop)
    initiatingPackageView.layout(paddingStart, initiatingTitleView.bottom + initiatingPackageView.marginTop)
    installingTitleView.layout(paddingStart, initiatingPackageView.bottom + installingTitleView.marginTop)
    installingPackageView.layout(paddingStart, installingTitleView.bottom + installingPackageView.marginTop)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
