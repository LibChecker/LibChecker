package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotDetailCountNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailCountView
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.text.NumberFormat

/**
 * <pre>
 * author : Absinthe
 * time : 2020/09/27
 * </pre>
 */
class SnapshotDetailCountAdapter : BaseQuickAdapter<SnapshotDetailCountNode, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(SnapshotDetailCountView(context))
  }

  override fun convert(holder: BaseViewHolder, item: SnapshotDetailCountNode) {
    (holder.itemView as SnapshotDetailCountView).apply {
      val countText = NumberFormat.getIntegerInstance().format(item.count)
      text = countText
      backgroundTintList = item.status.countColorRes.toColorStateList(context)
    }
  }
}
