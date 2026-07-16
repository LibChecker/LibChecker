package com.absinthe.libchecker.domain.snapshot.timenode.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotTimeNodeListData
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotTimeNodeItem
import com.absinthe.libchecker.domain.snapshot.timenode.ui.view.TimeNodeItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class TimeNodeAdapter : BaseQuickAdapter<SnapshotTimeNodeItem, BaseViewHolder>(0) {

  private var packageIconSources: Map<String, SnapshotPackageIconSource> = emptyMap()

  fun bind(listData: SnapshotTimeNodeListData) {
    packageIconSources = listData.packageIconSources
    setList(listData.items)
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(TimeNodeItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: SnapshotTimeNodeItem) {
    (holder.itemView as TimeNodeItemView).bind(item, packageIconSources)
  }
}
