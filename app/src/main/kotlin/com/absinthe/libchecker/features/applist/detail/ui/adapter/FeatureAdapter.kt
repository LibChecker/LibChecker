package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.features.applist.detail.bean.FeatureItem
import com.absinthe.libchecker.features.applist.detail.ui.view.FeatureLabelView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class FeatureAdapter : BaseQuickAdapter<FeatureItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(FeatureLabelView(context))
  }

  override fun convert(holder: BaseViewHolder, item: FeatureItem) {
    (holder.itemView as FeatureLabelView).setFeature(item)
  }
}
