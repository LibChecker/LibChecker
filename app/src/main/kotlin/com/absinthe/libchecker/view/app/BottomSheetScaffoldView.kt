package com.absinthe.libchecker.view.app

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
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

  /** Keep the list viewport full width and apply the sheet inset as content padding. */
  protected fun addFullWidthList(list: RecyclerView) {
    val start = paddingStart
    val end = paddingEnd
    setPaddingRelative(0, paddingTop, 0, paddingBottom)
    children.forEach { child ->
      child.updateLayoutParams<LayoutParams> {
        marginStart += start
        marginEnd += end
      }
    }
    list.setPaddingRelative(list.paddingStart + start, list.paddingTop, list.paddingEnd + end, list.paddingBottom)
    addView(list)
  }

  fun addContentView(content: View) {
    addView(content)
  }
}
