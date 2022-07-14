package com.absinthe.libchecker.recyclerview.adapter.detail

import android.view.ViewGroup
import com.absinthe.libchecker.view.detail.ProcessBarView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class ProcessBarAdapter : BaseQuickAdapter<ProcessBarAdapter.ProcessBarItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(ProcessBarView.ProcessBarItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: ProcessBarItem) {
    (holder.itemView as ProcessBarView.ProcessBarItemView).apply {
      setIndicatorColor(item.color)
      text.text = item.process
    }
  }

  data class ProcessBarItem(val process: String, val color: Int)
}
