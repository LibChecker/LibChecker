package com.absinthe.libchecker.features.snapshot.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.snapshot.detail.bean.ADDED
import com.absinthe.libchecker.features.snapshot.detail.bean.CHANGED
import com.absinthe.libchecker.features.snapshot.detail.bean.MOVED
import com.absinthe.libchecker.features.snapshot.detail.bean.REMOVED
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotDetailCountNode
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotDetailCountView
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

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
    val colorRes = when (item.status) {
      ADDED -> R.color.material_green_200
      REMOVED -> R.color.material_red_200
      CHANGED -> R.color.material_yellow_200
      MOVED -> R.color.material_blue_200
      else -> throw IllegalArgumentException("wrong diff type")
    }

    (holder.itemView as SnapshotDetailCountView).apply {
      text = item.count.toString()
      backgroundTintList = colorRes.toColorStateList(context)
    }
  }
}
