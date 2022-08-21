package com.absinthe.libchecker.view.applist

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.CheckableChipView

class AdvancedMenuItemView(context: Context) : FrameLayout(context) {
  val chip = CheckableChipView(context).also {
    it.layoutParams = MarginLayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also { lp ->
      lp.setMargins(4.dp, 4.dp, 4.dp, 4.dp)
    }
  }

  init {
    addView(chip)
  }
}
