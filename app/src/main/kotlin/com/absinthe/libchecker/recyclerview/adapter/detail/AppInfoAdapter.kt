package com.absinthe.libchecker.recyclerview.adapter.detail

import android.content.Intent
import android.content.pm.PackageItemInfo
import android.view.ViewGroup
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.view.detail.AppInfoItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */
class AppInfoAdapter : BaseQuickAdapter<AppInfoAdapter.AppInfoItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppInfoItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppInfoItem) {
    (holder.itemView as AppInfoItemView).apply {
      setIconBackground(LCAppUtils.getAppIcon(item.pii.packageName))
      setText(item.pii.loadLabel(context.packageManager))
    }
  }

  data class AppInfoItem(val pii: PackageItemInfo, val intent: Intent)
}
