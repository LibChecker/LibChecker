package com.absinthe.libchecker.recyclerview.adapter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.valueUnsafe
import com.absinthe.libchecker.view.statistics.LibReferenceItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LibReferenceAdapter : HighlightAdapter<LibReference>() {

  init {
    addChildClickViewIds(android.R.id.icon)
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      LibReferenceItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
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

  override fun convert(holder: BaseViewHolder, item: LibReference) {
    (holder.itemView as LibReferenceItemView).container.apply {
      count.text = item.referredList.size.toString()

      setOrHighlightText(libName, item.libName)

      item.chip?.let {
        icon.apply {
          setImageResource(it.iconRes)

          if (!GlobalValues.isColorfulIcon.valueUnsafe) {
            this.drawable.mutate().colorFilter =
              ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
          }
        }

        setOrHighlightText(labelName, it.name)
      } ?: let {
        icon.setImageResource(R.drawable.ic_question)
        val spannableString = SpannableString(context.getString(R.string.not_marked_lib))
        val colorSpanit = StyleSpan(Typeface.ITALIC)
        spannableString.setSpan(
          colorSpanit,
          0,
          spannableString.length,
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        labelName.text = spannableString
      }
    }
  }
}
