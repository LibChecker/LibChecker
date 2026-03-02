package com.absinthe.libchecker.features.snapshot.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.features.snapshot.ui.view.SystemPropItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class SystemPropsAdapter : BaseQuickAdapter<(Pair<String, String>), BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      SystemPropItemView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: Pair<String, String>) {
    (holder.itemView as SystemPropItemView).also {
      it.tvTitle.text = item.first
      it.tvText.text = item.second
    }
  }
}
