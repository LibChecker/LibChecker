package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.domain.app.AppInfoActionItem
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
      setIconBackground(getAppIcon(item.packageItemInfo.packageName))
      setText(item.packageItemInfo.loadLabel(context.packageManager))
    }
  }

  private fun getAppIcon(packageName: String): Drawable {
    return runCatching {
      PackageManagerCompat.getPackageInfo(
        packageName,
        0
      ).applicationInfo!!.loadIcon(SystemServices.packageManager)
    }.getOrDefault(Color.TRANSPARENT.toDrawable())
  }
}
