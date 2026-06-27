package com.absinthe.libchecker.features.settings.ui.adapter

import android.view.ViewGroup
import android.widget.LinearLayout
import com.absinthe.libchecker.domain.settings.GetUpdatesAction
import com.absinthe.libchecker.domain.settings.GetUpdatesItem
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.button.MaterialButton

class GetUpdatesAdapter : BaseQuickAdapter<GetUpdatesItem, BaseViewHolder>(0) {

  private var onActionClick: (GetUpdatesAction) -> Unit = {}

  fun setOnActionClickListener(listener: (GetUpdatesAction) -> Unit) {
    onActionClick = listener
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      MaterialButton(context).apply {
        layoutParams = LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: GetUpdatesItem) {
    (holder.itemView as MaterialButton).apply {
      setIconResource(item.iconRes)
      text = item.text
      setOnClickListener {
        onActionClick(item.action)
      }
    }
  }
}
