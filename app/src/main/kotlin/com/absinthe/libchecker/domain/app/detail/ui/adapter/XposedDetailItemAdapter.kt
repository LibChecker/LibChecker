package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.ui.view.XposedInfoBottomSheetView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class XposedDetailItemAdapter : BaseQuickAdapter<XposedInfoItemDisplay, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(XposedInfoBottomSheetView.XposedDetailItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: XposedInfoItemDisplay) {
    (holder.itemView as XposedInfoBottomSheetView.XposedDetailItemView).bind(item)
  }
}
