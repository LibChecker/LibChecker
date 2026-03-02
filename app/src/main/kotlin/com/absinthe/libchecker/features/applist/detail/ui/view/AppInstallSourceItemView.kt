package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.ui.view.AppItemView
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.view.AViewGroup

class AppInstallSourceItemView(context: Context) : AViewGroup(context) {

  val titleView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  val packageView = AppItemView(context).apply {
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
    addView(titleView)
    addView(packageView)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val parent = parent as ViewGroup
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    titleView.autoMeasure()
    packageView.let {
      it.measure(
        (measuredWidth - parent.paddingStart - parent.paddingEnd).toExactlyMeasureSpec(),
        if (it.isGone) 0 else it.defaultHeightMeasureSpec(parent)
      )
    }
    setMeasuredDimension(
      measuredWidth,
      titleView.marginTop +
        titleView.measuredHeight +
        packageView.marginTop +
        packageView.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    titleView.layout(paddingStart, titleView.marginTop)
    packageView.layout(paddingStart, titleView.bottom + packageView.marginTop)
  }
}
