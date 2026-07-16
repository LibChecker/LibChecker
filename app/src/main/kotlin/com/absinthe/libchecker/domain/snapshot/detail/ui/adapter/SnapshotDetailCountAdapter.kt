package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailCountRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailCountView
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/**
 * <pre>
 * author : Absinthe
 * time : 2020/09/27
 * </pre>
 */
class SnapshotDetailCountAdapter : BaseQuickAdapter<SnapshotDetailCountRenderState, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(SnapshotDetailCountView(context))
  }

  override fun convert(holder: BaseViewHolder, item: SnapshotDetailCountRenderState) {
    (holder.itemView as SnapshotDetailCountView).apply {
      text = item.text
      backgroundTintList = item.backgroundTintRes.toColorStateList(context)
    }
  }
}
