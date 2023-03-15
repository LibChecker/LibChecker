package com.absinthe.libchecker.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager

class SafeFlexboxLayoutManager : FlexboxLayoutManager {

  constructor(context: Context) : super(context)

  constructor(context: Context, flexDirection: Int) : super(context, flexDirection)

  constructor(context: Context, flexDirection: Int, flexWrap: Int) : super(
    context,
    flexDirection,
    flexWrap,
  )

  constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
    context,
    attrs,
    defStyleAttr,
    defStyleRes,
  )

  override fun generateLayoutParams(lp: ViewGroup.LayoutParams): RecyclerView.LayoutParams {
    return FlexboxLayoutManager.LayoutParams(lp)
  }
}
