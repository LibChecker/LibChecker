package com.absinthe.libchecker.recyclerview.adapter.detail

import android.view.ViewGroup
import com.absinthe.libchecker.model.SignatureDetailItem
import com.absinthe.libchecker.view.detail.SignatureDetailItemView
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
