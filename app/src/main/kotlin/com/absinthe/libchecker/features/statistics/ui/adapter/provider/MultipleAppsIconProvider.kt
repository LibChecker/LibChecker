package com.absinthe.libchecker.features.statistics.ui.adapter.provider

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.italic
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.features.statistics.bean.LibReference
import com.absinthe.libchecker.features.statistics.ui.adapter.LibReferenceAdapter
import com.absinthe.libchecker.features.statistics.ui.view.MultipleAppsIconItemView
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val MULTIPLE_APPS_ICON_PROVIDER = 1

class MultipleAppsIconProvider : BaseNodeProvider() {

  override val itemViewType: Int = MULTIPLE_APPS_ICON_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      MultipleAppsIconItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
          it.setMargins(0, margin, 0, margin)
        }
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    (helper.itemView as MultipleAppsIconItemView).container.apply {
      val libReferenceItem = item as LibReference
      icon.setIcons(libReferenceItem.referredList.toList())
      count.text = libReferenceItem.referredList.size.toString()
      labelName.text = buildSpannedString {
        italic {
          append(context.getString(R.string.not_marked_lib))
        }
      }

      if (item.type == PACKAGE) {
        setOrHighlightText(libName, libReferenceItem.libName + ".*")
      } else {
        setOrHighlightText(libName, libReferenceItem.libName)
      }
    }
  }

  private fun setOrHighlightText(view: TextView, text: CharSequence) {
    if (LibReferenceAdapter.highlightText.isNotBlank()) {
      view.tintHighlightText(LibReferenceAdapter.highlightText, text)
    } else {
      view.text = text
    }
  }
}
