package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider

import android.view.ViewGroup
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.BaseSnapshotNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SNAPSHOT_ITEM_PROVIDER
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotDetailNodeChipClickAction
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.itemRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.viewRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailItemView
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class SnapshotDetailItemProvider : BaseNodeProvider() {

  override val itemViewType: Int = SNAPSHOT_ITEM_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      SnapshotDetailItemView(context).also {
        it.layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    val node = item as BaseSnapshotNode
    val renderState = node.itemRenderState
    val chipClickAction = renderState.chipClickAction
    (helper.itemView as SnapshotDetailItemView).apply {
      render(renderState.viewRenderState)
      if (chipClickAction is SnapshotDetailNodeChipClickAction.OpenLibraryDetail) {
        setChipOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setChipOnClickListener
          }
          this@SnapshotDetailItemProvider.showSnapshotDetailLibraryDialog(chipClickAction.target)
        }
      } else {
        setChipOnClickListener(null)
      }
    }
  }
}
