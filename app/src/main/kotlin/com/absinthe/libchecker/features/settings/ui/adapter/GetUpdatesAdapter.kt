package com.absinthe.libchecker.features.settings.ui.adapter

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.LinearLayout
import com.absinthe.libchecker.features.settings.bean.GetUpdatesItem
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.button.MaterialButton

class GetUpdatesAdapter : BaseQuickAdapter<GetUpdatesItem, BaseViewHolder>(0) {

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
        context.startActivity(
          Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(item.url)
          }
        )
      }
    }
  }
}
