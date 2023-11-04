package com.absinthe.libchecker.features.snapshot.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getDrawable

class SnapshotDetailCountView(context: Context) : AppCompatTextView(context) {

  init {
    layoutParams = ViewGroup.MarginLayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.marginStart = 4.dp
      it.marginEnd = 4.dp
    }
    background = R.drawable.bg_snapshot_detail_count.getDrawable(context)
    setPadding(8.dp, 2.dp, 8.dp, 2.dp)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    setTextColor(R.color.material_grey_600.getColor(context))
  }
}
