package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class AppInstallTimeItemView(context: Context) : AViewGroup(context) {

  private val titleView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    text = context.getString(R.string.lib_detail_app_installed_time)
  }

  val contentView = ContentView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    isClickable = false
    isLongClickable = false
    isFocusable = false
    isFocusableInTouchMode = false
  }

  private val container = MaterialCardView(context).apply {
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
    container.addView(contentView)
    addView(container)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val parent = parent as ViewGroup
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    titleView.autoMeasure()
    container.let {
      it.measure(
        (measuredWidth - parent.paddingStart - parent.paddingEnd).toExactlyMeasureSpec(),
        if (it.isGone) 0 else it.defaultHeightMeasureSpec(parent)
      )
    }
    setMeasuredDimension(
      measuredWidth,
      titleView.marginTop +
        titleView.measuredHeight +
        container.marginTop +
        container.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    titleView.layout(paddingStart, titleView.marginTop)
    container.layout(paddingStart, titleView.bottom + container.marginTop)
  }

  class ContentView(context: Context) : AViewGroup(context) {
    val firstInstalledView = NativeLibItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      isClickable = false
      isLongClickable = false
      libName.text = context.getString(R.string.lib_detail_app_first_installed_time)
    }

    val lastUpdatedView = NativeLibItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      isClickable = false
      isLongClickable = false
      libName.text = context.getString(R.string.lib_detail_app_last_updated_time)
    }

    init {
      setPadding(0, 8.dp, 0, 8.dp)
      addView(firstInstalledView)
      addView(lastUpdatedView)
    }

    fun getAllContentText(): String {
      return listOf(
        firstInstalledView.libName.text,
        firstInstalledView.libSize.text,
        lastUpdatedView.libName.text,
        lastUpdatedView.libSize.text
      ).joinToString(System.lineSeparator())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      firstInstalledView.autoMeasure()
      lastUpdatedView.autoMeasure()
      setMeasuredDimension(
        measuredWidth,
        paddingTop +
          firstInstalledView.marginTop +
          firstInstalledView.measuredHeight +
          lastUpdatedView.marginTop +
          lastUpdatedView.measuredHeight +
          lastUpdatedView.marginBottom +
          paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      firstInstalledView.layout(paddingStart, firstInstalledView.marginTop + paddingTop)
      lastUpdatedView.layout(paddingStart, firstInstalledView.bottom + lastUpdatedView.marginTop)
    }
  }
}
