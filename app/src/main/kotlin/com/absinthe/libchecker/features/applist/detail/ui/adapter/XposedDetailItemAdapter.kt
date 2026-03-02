package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.features.applist.detail.ui.adapter.node.XposedDetailItem
import com.absinthe.libchecker.features.applist.detail.ui.view.XposedInfoBottomSheetView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class XposedDetailItemAdapter : BaseQuickAdapter<XposedDetailItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(XposedInfoBottomSheetView.XposedDetailItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: XposedDetailItem) {
    (holder.itemView as XposedInfoBottomSheetView.XposedDetailItemView).apply {
      icon.setImageResource(item.iconRes)
      tip.text = item.tip
      text.text = item.text
      text.setTextAppearance(item.textStyleRes)
    }
  }
}
