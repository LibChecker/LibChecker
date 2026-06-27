package com.absinthe.libchecker.domain.app.detail.ui.adapter

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.model.AppDetailToolbarItem
import com.absinthe.libchecker.domain.app.detail.ui.view.AppDetailToolbarView
import com.absinthe.libchecker.utils.OsUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppDetailToolbarAdapter : BaseQuickAdapter<AppDetailToolbarItem, BaseViewHolder>(0) {

  init {
    setHasStableIds(true)
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppDetailToolbarView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppDetailToolbarItem) {
    (holder.itemView as AppDetailToolbarView).apply {
      val label = context.getString(item.tooltipTextRes)
      setImageResource(item.icon)
      contentDescription = label
      setOnClickListener { item.onClick() }

      if (OsUtils.atLeastO()) {
        tooltipText = label
      }
    }
  }

  override fun getItemId(position: Int): Long {
    return data[position].hashCode().toLong()
  }
}
