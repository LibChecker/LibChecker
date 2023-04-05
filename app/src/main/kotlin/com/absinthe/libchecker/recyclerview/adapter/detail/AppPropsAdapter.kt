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

      if (maybeResourceId(item.value)) {
        setValueText(resIdToPath(item.value))
      } else {
        setValueText(item.value)
      }
    }
  }

  private fun maybeResourceId(idText: String): Boolean {
    if (idText.isNotBlank() && idText.isDigitsOnly() && idText.toLongOrNull() != null) {
      val id = idText.toLong()
      @Suppress("KotlinConstantConditions")
      if ((id and 0xFF000000) == 0x7F000000.toLong() && (id and 0x00FF0000) >= 0x00010000 && (id and 0x0000FFFF) >= 0x00000000) {
        // This may be an android resource id
        return true
      }
    }
    return false
  }

  private fun resIdToPath(idText: String): String {
    val id = idText.toInt()
    return runCatching { appResources.getResourceName(id) }.getOrDefault(idText)
  }

  companion object {
    val tipsMap = mapOf<String, String>(

    )
  }
}
