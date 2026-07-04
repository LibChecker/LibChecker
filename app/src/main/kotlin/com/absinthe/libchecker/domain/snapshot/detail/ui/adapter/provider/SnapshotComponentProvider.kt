package com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.provider

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SNAPSHOT_COMPONENT_PROVIDER
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotComponentNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotDetailNodeChipClickAction
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.cardRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.itemRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailComponentView
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class SnapshotComponentProvider : BaseNodeProvider() {

  override val itemViewType: Int = SNAPSHOT_COMPONENT_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      SnapshotDetailComponentView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).also {
        it.layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    (helper.itemView as SnapshotDetailComponentView).apply {
      val node = item as SnapshotComponentNode
      val renderState = node.itemRenderState
      val chipClickAction = renderState.chipClickAction

      render(renderState.cardRenderState)
      if (chipClickAction is SnapshotDetailNodeChipClickAction.OpenLibraryDetail) {
        setChipOnClickListener {
          if (AntiShakeUtils.isInvalidClick(it)) {
            return@setChipOnClickListener
          }
          this@SnapshotComponentProvider.showSnapshotDetailLibraryDialog(chipClickAction.target)
        }
      } else {
        setChipOnClickListener(null)
      }
    }
  }
}
