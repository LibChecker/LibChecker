package com.absinthe.libchecker.domain.snapshot.album.ui.adapter

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.LinearLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.album.model.AlbumItemDisplayData
import com.absinthe.libchecker.domain.snapshot.album.ui.view.AlbumItemView
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AlbumAdapter : BaseQuickAdapter<AlbumItemDisplayData, BaseViewHolder>(0) {
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

  override fun convert(holder: BaseViewHolder, item: AlbumItemDisplayData) {
    val albumItemView = (holder.itemView as LinearLayout).let { container ->
      if (container.childCount == 0) {
        container.addView(createAlbumItemView())
      }
      container.getChildAt(0) as AlbumItemView
    }
    albumItemView.apply {
      render(item)
      setOnClickListener {
        holder.itemView.performClick()
      }
    }
  }

  private fun createAlbumItemView(): AlbumItemView {
    return AlbumItemView(ContextThemeWrapper(context, R.style.AlbumMaterialCard)).apply {
      layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        val marginHorizontal =
          context.getDimensionPixelSize(R.dimen.album_item_margin_horizontal)
        val marginVertical = context.getDimensionPixelSize(R.dimen.album_item_margin_vertical)
        it.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical)
      }
    }
  }
}
