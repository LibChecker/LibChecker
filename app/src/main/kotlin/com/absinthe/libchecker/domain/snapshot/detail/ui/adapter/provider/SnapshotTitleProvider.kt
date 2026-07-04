package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetailCountAdapter
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
    val itemView = (helper.itemView as SnapshotDetailTitleView).container
    val node = item as SnapshotTitleNode
    val renderState = node.titleRenderState
    val countAdapter = SnapshotDetailCountAdapter()

    itemView.title.text = renderState.title
    itemView.list.apply {
      adapter = countAdapter
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
      if (parent == null) {
        itemView.addView(this)
      }
    }

    countAdapter.setList(renderState.counts)
    helper.itemView.contentDescription = renderState.contentDescription

    onExpansionToggled(itemView.arrow, renderState.expanded)
  }

  override fun onClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
    getAdapter()?.expandOrCollapse(position)
  }

  private fun onExpansionToggled(arrow: ImageView, expanded: Boolean) {
    val start: Float
    val target: Float

    if (expanded) {
      start = 0f
      target = 90f
    } else {
      start = 90f
      target = 0f
    }

    ObjectAnimator.ofFloat(arrow, View.ROTATION, start, target).apply {
      duration = 200
      start()
    }
  }
}
