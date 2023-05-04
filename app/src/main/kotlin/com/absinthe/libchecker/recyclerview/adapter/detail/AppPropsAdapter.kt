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
      setValueText(parseValue(item))
    }
  }

  private val category = mapOf(
    0 to "game",
    1 to "audio",
    2 to "video",
    3 to "image",
    4 to "social",
    5 to "news",
    6 to "maps",
    7 to "productivity",
    8 to "accessibility"
  )

  private fun parseValue(item: AppPropItem): String {
    return when {
      item.key == "appCategory" -> {
        category.getValue(item.value.toInt())
      }
      item.value.isNotBlank() && item.value.isDigitsOnly() && item.value.toLongOrNull() != null -> {
        runCatching { appResources.getResourceName(item.value.toInt()) }.getOrDefault(item.value)
      }
      else -> item.value
    }
  }
}
