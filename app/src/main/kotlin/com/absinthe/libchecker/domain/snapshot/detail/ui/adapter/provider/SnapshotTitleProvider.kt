package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider

import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SNAPSHOT_TITLE_PROVIDER
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotTitleNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.titleRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailTitleView
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class SnapshotTitleProvider : BaseNodeProvider() {

  override val itemViewType: Int = SNAPSHOT_TITLE_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(SnapshotDetailTitleView(context))
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    val node = item as SnapshotTitleNode
    (helper.itemView as SnapshotDetailTitleView).render(node.titleRenderState)
  }

  override fun onClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
    getAdapter()?.expandOrCollapse(position)
  }
}
