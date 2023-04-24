package com.absinthe.libchecker.recyclerview.adapter.detail

import android.content.pm.PackageInfo
import android.view.ViewGroup
import androidx.core.text.isDigitsOnly
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.model.AppPropItem
import com.absinthe.libchecker.view.detail.AppPropItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppPropsAdapter(packageInfo: PackageInfo) : BaseQuickAdapter<AppPropItem, BaseViewHolder>(0) {

  private val appResources by lazy {
    SystemServices.packageManager.getResourcesForApplication(
      packageInfo.applicationInfo
    )
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppPropItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppPropItem) {
    (holder.itemView as AppPropItemView).apply {
      // TODO
      // setTipText("TODO")
      setKeyText(item.key)
      setValueText(resIdToPath(item.value))
    }
  }

  private fun resIdToPath(idText: String): String {
    return if (idText.isNotBlank() && idText.isDigitsOnly() && idText.toLongOrNull() != null) {
      runCatching { appResources.getResourceName(idText.toInt()) }.getOrDefault(idText)
    } else {
      idText
    }
  }
}
