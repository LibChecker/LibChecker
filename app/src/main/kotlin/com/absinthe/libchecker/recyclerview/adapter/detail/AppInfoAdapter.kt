package com.absinthe.libchecker.recyclerview.adapter.detail

import android.content.Intent
import android.content.pm.PackageItemInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
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
      setIconBackground(getAppIcon(item.pii.packageName))
      setText(item.pii.loadLabel(context.packageManager))
    }
  }

  private fun getAppIcon(packageName: String): Drawable {
    return runCatching {
      PackageManagerCompat.getPackageInfo(
        packageName,
        0
      ).applicationInfo.loadIcon(SystemServices.packageManager)
    }.getOrDefault(ColorDrawable(Color.TRANSPARENT))
  }

  data class AppInfoItem(val pii: PackageItemInfo, val intent: Intent)
}
