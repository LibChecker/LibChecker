package com.absinthe.libchecker.domain.about.ui.adapter

import android.view.ViewGroup
import android.widget.LinearLayout
import com.absinthe.libchecker.domain.about.model.DeveloperInfo
import com.absinthe.libchecker.domain.about.model.DevelopersDialogAction
import com.absinthe.libchecker.domain.about.model.toDevelopersDialogAction
import com.absinthe.libchecker.domain.about.ui.view.DeveloperItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class DeveloperInfoAdapter(
  private val onAction: (DevelopersDialogAction) -> Unit
) : BaseQuickAdapter<DeveloperInfo, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      DeveloperItemView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: DeveloperInfo) {
    (holder.itemView as DeveloperItemView).bind(item) {
      onAction(item.toDevelopersDialogAction())
    }
  }
}
