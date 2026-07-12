package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.domain.app.detail.model.FeatureItem
import com.absinthe.libchecker.domain.app.detail.ui.view.FeatureLabelView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class FeatureAdapter(
  private val onItemClick: (FeatureItem) -> Unit
) : BaseQuickAdapter<FeatureItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(FeatureLabelView(context))
  }

  override fun convert(holder: BaseViewHolder, item: FeatureItem) {
    (holder.itemView as FeatureLabelView).bind(item) {
      onItemClick(item)
    }
  }

  override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
    (holder.itemView as? FeatureLabelView)?.recycle()
    super.onViewRecycled(holder)
  }
}
