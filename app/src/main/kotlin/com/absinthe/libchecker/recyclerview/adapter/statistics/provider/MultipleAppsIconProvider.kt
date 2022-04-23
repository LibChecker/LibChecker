package com.absinthe.libchecker.recyclerview.adapter.statistics.provider

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.recyclerview.adapter.statistics.LibReferenceAdapter
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.absinthe.libchecker.view.statistics.MultipleAppsIconItemView
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val MULTIPLE_APPS_ICON_PROVIDER = 1

class MultipleAppsIconProvider(val lifecycleScope: LifecycleCoroutineScope) : BaseNodeProvider() {

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
          it.setMargins(margin, margin, margin, margin)
        }
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    (helper.itemView as MultipleAppsIconItemView).container.apply {
      val libReferenceItem = item as LibReference
      icon.setIcons(lifecycleScope, libReferenceItem.referredList.toList())
      count.text = libReferenceItem.referredList.size.toString()
      val spannableString = SpannableString(context.getString(R.string.not_marked_lib))
      val colorSpanit = StyleSpan(Typeface.ITALIC)
      spannableString.setSpan(
        colorSpanit,
        0,
        spannableString.length,
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
      )
      labelName.text = spannableString
      setOrHighlightText(libName, libReferenceItem.libName)
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
