package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.model.ProcessBarItemRenderState
import com.absinthe.libchecker.domain.app.detail.ui.view.ProcessBarView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class ProcessBarAdapter : BaseQuickAdapter<ProcessBarItemRenderState, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(ProcessBarView.ProcessBarItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: ProcessBarItemRenderState) {
    (holder.itemView as ProcessBarView.ProcessBarItemView).bind(item)
  }
}
