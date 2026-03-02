package com.absinthe.libchecker.features.album.track.ui.adapter

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.album.track.bean.TrackListItem
import com.absinthe.libchecker.features.album.track.ui.view.TrackItemView
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class TrackAdapter : BaseQuickAdapter<TrackListItem, BaseViewHolder>(0) {

  init {
    addChildClickViewIds(android.R.id.toggle)
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      TrackItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
          it.setMargins(0, margin, 0, margin)
        }
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: TrackListItem) {
    (holder.itemView as TrackItemView).container.apply {
      val packageInfo = runCatching {
        PackageUtils.getPackageInfo(item.packageName)
      }.getOrNull() ?: return

      icon.load(packageInfo)
      appName.text = item.label
      packageName.text = item.packageName
      switch.isChecked = item.switchState
    }
  }
}
