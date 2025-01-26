package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.features.applist.detail.bean.AppPropItem
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_TEXT
import com.absinthe.libchecker.features.applist.detail.ui.XmlBSDFragment
import com.absinthe.libchecker.features.applist.detail.ui.view.AppPropItemView
import com.absinthe.libchecker.utils.extensions.maybeResourceId
import com.absinthe.libchecker.utils.manifest.PropertiesMap
import com.absinthe.libchecker.utils.manifest.ResourceParser
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import timber.log.Timber

class AppPropsAdapter(
  packageInfo: PackageInfo,
  private val fragMgr: FragmentManager
) : BaseQuickAdapter<AppPropItem, BaseViewHolder>(0) {

  private val appResources by lazy {
    SystemServices.packageManager.getResourcesForApplication(
      packageInfo.applicationInfo!!
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

  private val linkable = setOf("array", "bool", "color", "dimen", "drawable", "integer", "mipmap", "string", "xml")

  private fun parseValue(item: AppPropItem): String {
    if (item.value.maybeResourceId()) {
      try {
        return appResources.getResourceName(item.value.toInt())
      } catch (_: Exception) {}
    }
    return PropertiesMap.parseProperty(item.key, item.value)
  }

  private fun initLinkBtn(itemView: AppPropItemView, item: AppPropItem) {
    if (!item.value.maybeResourceId()) {
      itemView.linkToIcon.isVisible = false
      return
    }

    val type = runCatching {
      appResources.getResourceTypeName(item.value.toInt())
    }.getOrDefault("null")
    if (!linkable.contains(type)) {
      itemView.linkToIcon.isVisible = false
      return
    }

    itemView.linkToIcon.apply {
      isVisible = true
      setImageResource(R.drawable.ic_outline_change_circle_24)
      setOnClickListener {
        val transformed = getTag(R.id.resource_transformed_id) as? Boolean == true
        if (transformed) {
          itemView.value.text = parseValue(item)
          setImageResource(R.drawable.ic_outline_change_circle_24)
          setTag(R.id.resource_transformed_id, false)
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
                appResources.getDrawable(item.value.toInt(), null)?.let { drawable ->
                  val bitmap = drawable.toBitmap(
                    itemView.linkToIcon.measuredWidth,
                    itemView.linkToIcon.measuredHeight,
                    Bitmap.Config.ARGB_8888
                  )
                  setImageBitmap(bitmap)
                }
                clickedTag = true
              }

              "color" -> {
                appResources.getColor(item.value.toInt(), null).let { colorInt ->
                  val bitmap = ShapeDrawable(OvalShape()).apply {
                    paint.color = colorInt
                  }.toBitmap(
                    itemView.linkToIcon.measuredWidth,
                    itemView.linkToIcon.measuredHeight,
                    Bitmap.Config.ARGB_8888
                  )
                  setImageBitmap(bitmap)
                }
                clickedTag = true
              }

              "dimen" -> {
                itemView.value.text = appResources.getDimension(item.value.toInt()).toString()
                clickedTag = true
              }

              "integer" -> {
                itemView.value.text = appResources.getInteger(item.value.toInt()).toString()
                clickedTag = true
              }

              else -> {
                clickedTag = false
              }
            }
          }
          setTag(R.id.resource_transformed_id, clickedTag)
        }
      }
    }
  }
}
