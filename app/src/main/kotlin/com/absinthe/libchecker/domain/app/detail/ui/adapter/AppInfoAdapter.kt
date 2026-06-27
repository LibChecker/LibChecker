package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.graphics.Color
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.absinthe.libchecker.domain.app.detail.action.AppInfoActionItem
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInfoItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */
class AppInfoAdapter : BaseQuickAdapter<AppInfoActionItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppInfoItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppInfoActionItem) {
    (holder.itemView as AppInfoItemView).apply {
      setIconBackground(item.icon ?: Color.TRANSPARENT.toDrawable())
      setText(item.label)
    }
  }
}
