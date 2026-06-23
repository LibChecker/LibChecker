package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.features.applist.detail.bean.AppPropItem
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_TEXT
import com.absinthe.libchecker.features.applist.detail.ui.XmlBSDFragment
import com.absinthe.libchecker.features.applist.detail.ui.view.AppPropItemView
import com.absinthe.libchecker.utils.manifest.ResourceParser
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import timber.log.Timber

class AppPropsAdapter(
  ai: ApplicationInfo?,
  private val fragMgr: FragmentManager
) : BaseQuickAdapter<AppPropItem, BaseViewHolder>(0) {

  private val appResources by lazy {
    runCatching { SystemServices.packageManager.getResourcesForApplication(ai!!) }
      .onFailure { Timber.e(it) }
      .getOrNull()
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(AppPropItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: AppPropItem) {
    (holder.itemView as AppPropItemView).apply {
      // TODO
      // setTipText("TODO")
      key.text = item.key
      value.text = item.displayValue
      contentDescription = buildItemDescription(key.text, value.text)
      initLinkBtn(this, item)
    }
  }

  private val linkable = setOf("array", "bool", "color", "dimen", "drawable", "integer", "mipmap", "string", "xml")

  private fun initLinkBtn(itemView: AppPropItemView, item: AppPropItem) {
    val resourceId = item.resourceId
    val type = item.resourceType
    if (resourceId == null || type == null || !linkable.contains(type)) {
      itemView.linkToIcon.isVisible = false
      itemView.linkToIcon.setTag(R.id.resource_transformed_id, false)
      return
    }

    itemView.linkToIcon.apply {
      isVisible = true
      setImageResource(R.drawable.ic_outline_change_circle_24)
      setTag(R.id.resource_transformed_id, false)
      setOnClickListener {
        val transformed = getTag(R.id.resource_transformed_id) as? Boolean == true
        if (transformed) {
          itemView.value.text = item.displayValue
          itemView.contentDescription = buildItemDescription(itemView.key.text, itemView.value.text)
          setImageResource(R.drawable.ic_outline_change_circle_24)
          setTag(R.id.resource_transformed_id, false)
        } else {
          val appResources = this@AppPropsAdapter.appResources ?: return@setOnClickListener
          var clickedTag = false
          Timber.d("type: $type")
          runCatching {
            when (type) {
              "string" -> {
                itemView.value.text = appResources.getString(resourceId)
                clickedTag = true
              }

              "array" -> {
                itemView.value.text =
                  appResources.getStringArray(resourceId).contentToString()
                clickedTag = true
              }

              "bool" -> {
                itemView.value.text = appResources.getBoolean(resourceId).toString()
                clickedTag = true
              }

              "xml" -> {
                appResources.getXml(resourceId).let {
                  val text = ResourceParser(it).setMarkColor(true).parse()
                  XmlBSDFragment().apply {
                    arguments = Bundle().apply {
                      putCharSequence(EXTRA_TEXT, text)
                    }
                    show(fragMgr, XmlBSDFragment::class.java.name)
                  }
                }
                clickedTag = false
              }

              "drawable", "mipmap" -> {
                appResources.getDrawable(resourceId, null)?.let { drawable ->
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
                appResources.getColor(resourceId, null).let { colorInt ->
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
                itemView.value.text = appResources.getDimension(resourceId).toString()
                clickedTag = true
              }

              "integer" -> {
                itemView.value.text = appResources.getInteger(resourceId).toString()
                clickedTag = true
              }

              else -> {
                clickedTag = false
              }
            }
          }
          itemView.contentDescription = buildItemDescription(itemView.key.text, itemView.value.text)
          setTag(R.id.resource_transformed_id, clickedTag)
        }
      }
    }
  }

  private fun buildItemDescription(vararg parts: CharSequence?): String {
    return parts
      .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
      .joinToString()
  }
}
