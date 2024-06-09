package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.features.applist.ui.view.AppItemView
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class OverlayDetailBottomSheetView(context: Context) :
  AViewGroup(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = Constants.OVERLAY_STRING
  }

  val detailsTitleView = DetailsTitleView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
  }

  private val targetTitleView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    //noinspection SetTextI18n
    text = "Target Package"
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  val targetPackageView = AppItemView(context).apply {
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
    addView(detailsTitleView)
    addView(targetTitleView)
    addView(targetPackageView)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.autoMeasure()
    detailsTitleView.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      detailsTitleView.defaultHeightMeasureSpec(this)
    )
    targetTitleView.autoMeasure()
    targetPackageView.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      targetPackageView.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        header.measuredHeight +
        detailsTitleView.marginTop +
        detailsTitleView.measuredHeight +
        targetTitleView.marginTop +
        targetTitleView.measuredHeight +
        targetPackageView.marginTop +
        targetPackageView.measuredHeight +
        paddingBottom
    )
  }

  override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
    header.layout(0, paddingTop)
    detailsTitleView.layout(paddingStart, header.bottom + detailsTitleView.marginTop)
    targetTitleView.layout(paddingStart, detailsTitleView.bottom + targetTitleView.marginTop)
    targetPackageView.layout(paddingStart, targetTitleView.bottom + targetPackageView.marginTop)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
