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
import com.absinthe.libchecker.extensions.px
import com.absinthe.libchecker.extensions.tintHighlightText
import com.absinthe.libchecker.extensions.valueUnsafe
import com.absinthe.libchecker.view.statistics.LibReferenceItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LibReferenceAdapter : BaseQuickAdapter<LibReference, BaseViewHolder>(0) {

    var highlightText: String = ""

    init {
        addChildClickViewIds(android.R.id.icon)
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(
            LibReferenceItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    val margin = R.dimen.main_card_margin.px
                    it.setMargins(margin, margin, margin, margin)
                }
            }
        )
    }

    override fun convert(holder: BaseViewHolder, item: LibReference) {
        (holder.itemView as LibReferenceItemView).container.apply {
            count.text = item.referredCount.toString()

            if (highlightText.isNotBlank()) {
                libName.tintHighlightText(highlightText, item.libName)
            } else {
                libName.text = item.libName
            }

            item.chip?.let {
                icon.apply {
                    setImageResource(it.iconRes)

                    if (!GlobalValues.isColorfulIcon.valueUnsafe) {
                        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                    }
                }

                if (highlightText.isNotBlank()) {
                    labelName.tintHighlightText(highlightText, it.name)
                } else {
                    labelName.text = it.name
                }
            } ?: let {
                icon.setImageResource(R.drawable.ic_question)
                val spannableString = SpannableString(context.getString(R.string.not_marked_lib))
                val colorSpanit = StyleSpan(Typeface.ITALIC)
                spannableString.setSpan(colorSpanit, 0, spannableString.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                labelName.text = spannableString
            }
        }
    }
}