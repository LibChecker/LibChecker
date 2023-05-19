package com.absinthe.libchecker.recyclerview.adapter.detail

import android.content.pm.PackageInfo
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.model.AppPropItem
import com.absinthe.libchecker.ui.fragment.detail.EXTRA_TEXT
import com.absinthe.libchecker.ui.fragment.detail.XmlBSDFragment
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.manifest.PropertiesMap
import com.absinthe.libchecker.utils.manifest.ResourceParser
import com.absinthe.libchecker.view.detail.AppPropItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import timber.log.Timber

class AppPropsAdapter(
  packageInfo: PackageInfo,
  private val fragMgr: FragmentManager
) : BaseQuickAdapter<AppPropItem, BaseViewHolder>(0) {

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
      key.text = item.key
      value.text = parseValue(item)
      initLinkBtn(this, item)
    }
  }

  private val linkable = setOf("string", "array", "bool", "xml", "drawable", "mipmap", "color", "dimen")

  private fun parseValue(item: AppPropItem): String {
    if (item.value.maybeResourceId()) {
      try {
        return appResources.getResourceName(item.value.toInt())
      } catch (_: Exception) {}
    }
    return PropertiesMap.parseProperty(item.key, item.value)
  }

  private fun initLinkBtn(itemView: AppPropItemView, item: AppPropItem) {
    if (!item.value.maybeResourceId()) return

    val type = runCatching {
      appResources.getResourceTypeName(item.value.toInt())
    }.getOrDefault("null")
    itemView.linkToIcon.isVisible = linkable.contains(type)

    if (itemView.linkToIcon.isVisible) {
      itemView.linkToIcon.setOnClickListener {
        val transformed = itemView.linkToIcon.getTag(R.id.resource_transformed_id) as? Boolean ?: false
        if (transformed) {
          itemView.value.text = parseValue(item)
          itemView.linkToIcon.setImageResource(R.drawable.ic_outline_change_circle_24)
          itemView.linkToIcon.setTag(R.id.resource_transformed_id, false)
        } else {
          var clickedTag = false
          Timber.d("type: $type")
          runCatching {
            when (type) {
              "string" -> {
                itemView.value.text = appResources.getString(item.value.toInt())
                clickedTag = true
              }
              "array" -> {
                itemView.value.text =
                  appResources.getStringArray(item.value.toInt()).contentToString()
                clickedTag = true
              }
              "bool" -> {
                itemView.value.text = appResources.getBoolean(item.value.toInt()).toString()
                clickedTag = true
              }
              "xml" -> {
                appResources.getXml(item.value.toInt()).let {
                  val text = ResourceParser(it).setMarkColor(true).parse()
                  XmlBSDFragment().apply {
                    arguments = bundleOf(
                      EXTRA_TEXT to text
                    )
                    show(fragMgr, XmlBSDFragment::class.java.name)
                  }
                }
                clickedTag = false
              }
              "drawable", "mipmap" -> {
                itemView.linkToIcon.setImageDrawable(
                  appResources.getDrawable(
                    item.value.toInt(),
                    null
                  )
                )
                clickedTag = true
              }
              "color" -> {
                itemView.linkToIcon.setImageDrawable(
                  ColorDrawable(
                    appResources.getColor(
                      item.value.toInt(),
                      null
                    )
                  )
                )
                clickedTag = true
              }
              "dimen" -> {
                itemView.value.text = appResources.getDimension(item.value.toInt()).toString()
                clickedTag = true
              }
              else -> {
                clickedTag = false
              }
            }
          }
          itemView.linkToIcon.setTag(R.id.resource_transformed_id, clickedTag)
        }
      }
    }
  }
}
