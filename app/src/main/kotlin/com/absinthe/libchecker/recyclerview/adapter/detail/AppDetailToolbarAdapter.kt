package com.absinthe.libchecker.recyclerview.adapter.detail

import android.view.ViewGroup
import com.absinthe.libchecker.bean.AppDetailToolbarItem
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.view.detail.AppDetailToolbarView
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
      setImageResource(item.icon)
      setOnClickListener { item.onClick() }

      if (OsUtils.atLeastO()) {
        tooltipText = context.getString(item.tooltipTextRes)
      }
    }
  }

  override fun getItemId(position: Int): Long {
    return data[position].hashCode().toLong()
  }
}
