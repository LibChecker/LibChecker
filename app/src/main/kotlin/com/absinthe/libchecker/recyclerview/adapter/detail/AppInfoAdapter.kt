package com.absinthe.libchecker.recyclerview.adapter.detail

import android.content.pm.ResolveInfo
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
class AppInfoAdapter : BaseQuickAdapter<ResolveInfo, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppInfoItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: ResolveInfo) {
    (holder.itemView as AppInfoItemView).apply {
      setIconBackground(LCAppUtils.getAppIcon(item.activityInfo.packageName))
      setText(item.activityInfo.loadLabel(context.packageManager))
    }
  }
}
