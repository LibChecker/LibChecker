package com.absinthe.libchecker.domain.snapshot.list.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SystemPropItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class SystemPropsAdapter : BaseQuickAdapter<SnapshotSystemPropDisplayData, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      SystemPropItemView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: SnapshotSystemPropDisplayData) {
    (holder.itemView as SystemPropItemView).also {
      it.tvTitle.text = item.label
      it.tvText.text = item.displayValue
      it.contentDescription = listOf(item.label, item.displayValue).joinToString()
    }
  }
}
