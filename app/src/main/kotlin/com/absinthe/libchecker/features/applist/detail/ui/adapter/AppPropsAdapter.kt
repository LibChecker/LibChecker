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
import com.absinthe.libchecker.domain.app.ResolveAppResourceValueUseCase
import com.absinthe.libchecker.domain.app.ResolveAppResourceValueUseCase.AppResourceValue
import com.absinthe.libchecker.domain.app.detail.model.AppPropItem
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_TEXT
import com.absinthe.libchecker.features.applist.detail.ui.XmlBSDFragment
import com.absinthe.libchecker.features.applist.detail.ui.view.AppPropItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppPropsAdapter(
  private val applicationInfo: ApplicationInfo?,
  private val fragMgr: FragmentManager,
  private val resolveAppResourceValue: ResolveAppResourceValueUseCase
) : BaseQuickAdapter<AppPropItem, BaseViewHolder>(0) {

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

  private fun initLinkBtn(itemView: AppPropItemView, item: AppPropItem) {
    val resourceId = item.resourceId
    val type = item.resourceType
    if (resourceId == null || !ResolveAppResourceValueUseCase.isLinkableType(type)) {
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
          val clickedTag = when (
            val resourceValue = resolveAppResourceValue(
              ResolveAppResourceValueUseCase.Request(
                applicationInfo = applicationInfo,
                resourceId = resourceId,
                resourceType = type
              )
            )
          ) {
            is AppResourceValue.Text -> {
              itemView.value.text = resourceValue.value
              true
            }

            is AppResourceValue.Xml -> {
              XmlBSDFragment().apply {
                arguments = Bundle().apply {
                  putCharSequence(EXTRA_TEXT, resourceValue.value)
                }
                show(fragMgr, XmlBSDFragment::class.java.name)
              }
              false
            }

            is AppResourceValue.DrawablePreview -> {
              val bitmap = resourceValue.drawable.toBitmap(
                itemView.linkToIcon.measuredWidth,
                itemView.linkToIcon.measuredHeight,
                Bitmap.Config.ARGB_8888
              )
              setImageBitmap(bitmap)
              true
            }

            is AppResourceValue.ColorPreview -> {
              val bitmap = ShapeDrawable(OvalShape()).apply {
                paint.color = resourceValue.color
              }.toBitmap(
                itemView.linkToIcon.measuredWidth,
                itemView.linkToIcon.measuredHeight,
                Bitmap.Config.ARGB_8888
              )
              setImageBitmap(bitmap)
              true
            }

            null -> {
              false
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
