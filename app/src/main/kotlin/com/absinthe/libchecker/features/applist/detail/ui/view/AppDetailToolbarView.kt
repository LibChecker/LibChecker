package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr

class AppDetailToolbarView(context: Context) : AppCompatImageButton(context) {

  init {
    val size = context.getDimensionPixelSize(R.dimen.detail_toolbar_size)
    layoutParams = ViewGroup.LayoutParams(size, size)
    scaleType = ScaleType.CENTER
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackgroundBorderless))
  }
}
