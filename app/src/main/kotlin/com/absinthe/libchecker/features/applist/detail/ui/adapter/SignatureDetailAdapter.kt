package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.features.applist.detail.bean.SignatureDetailItem
import com.absinthe.libchecker.features.applist.detail.ui.view.SignatureDetailItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class SignatureDetailAdapter : BaseQuickAdapter<SignatureDetailItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(SignatureDetailItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: SignatureDetailItem) {
    (holder.itemView as SignatureDetailItemView).apply {
      type.text = item.type
      content.text = item.content
    }
  }
}
