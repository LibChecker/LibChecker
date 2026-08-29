package com.absinthe.libchecker.view.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

open class BottomSheetScaffoldView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  protected val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
  }

  init {
    orientation = VERTICAL
    addView(header)
  }

  override fun getHeaderView(): BottomSheetHeaderView = header

  fun addContentView(content: View) {
    addView(content)
  }
}
