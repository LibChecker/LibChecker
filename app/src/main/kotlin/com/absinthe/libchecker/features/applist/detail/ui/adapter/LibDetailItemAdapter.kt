package com.absinthe.libchecker.features.applist.detail.ui.adapter

import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import com.absinthe.libchecker.features.applist.detail.ui.adapter.node.LibDetailItem
import com.absinthe.libchecker.features.applist.detail.ui.view.LibDetailBottomSheetView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LibDetailItemAdapter : BaseQuickAdapter<LibDetailItem, BaseViewHolder>(0) {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(LibDetailBottomSheetView.LibDetailItemView(context))
  }

  override fun convert(holder: BaseViewHolder, item: LibDetailItem) {
    (holder.itemView as LibDetailBottomSheetView.LibDetailItemView).apply {
      icon.setImageResource(item.iconRes)
      tip.text = context.getString(item.tipRes)
      text.setTextAppearance(item.textStyleRes)
      if (item.text.startsWith("<a href")) {
        text.apply {
          isClickable = true
          movementMethod = LinkMovementMethod.getInstance()
          text = HtmlCompat.fromHtml(item.text, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
      } else {
        text.apply {
          isClickable = false
          movementMethod = null
          text = item.text
        }
      }
    }
  }
}
