package com.absinthe.libchecker.features.home.ui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AdvancedMenuAdapter : BaseQuickAdapter<View, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.VERTICAL
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: View) {
    (holder.itemView as LinearLayout).apply {
      if (item.parent == null) {
        addView(item)
      }
    }
  }
}
