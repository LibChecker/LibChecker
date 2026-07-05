package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.ui.adapter.node.LibDetailItem
import com.absinthe.libchecker.domain.app.detail.ui.view.LibDetailBottomSheetView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LibDetailItemAdapter : BaseQuickAdapter<LibDetailItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(LibDetailBottomSheetView.LibDetailItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: LibDetailItem) {
    (holder.itemView as LibDetailBottomSheetView.LibDetailItemView).bind(item)
  }
}
