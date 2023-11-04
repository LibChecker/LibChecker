package com.absinthe.libchecker.features.album.ui.adapter

import android.view.ViewGroup
import android.widget.LinearLayout
import com.absinthe.libchecker.features.album.ui.view.AlbumItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AlbumAdapter : BaseQuickAdapter<AlbumItemView, BaseViewHolder>(0) {
  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.VERTICAL
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: AlbumItemView) {
    (holder.itemView as LinearLayout).apply {
      if (item.parent == null) {
        addView(item)
      }
    }
  }
}
