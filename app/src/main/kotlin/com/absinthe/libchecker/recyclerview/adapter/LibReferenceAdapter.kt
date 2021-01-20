package com.absinthe.libchecker.recyclerview.adapter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.ViewGroup
import android.widget.ImageButton
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.extensions.valueUnsafe
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.zhangyue.we.x2c.X2C
import com.zhangyue.we.x2c.ano.Xml


@Xml(layouts = ["item_lib_reference"])
class LibReferenceAdapter : BaseQuickAdapter<LibReference, BaseViewHolder>(0) {

    init {
        addChildClickViewIds(R.id.iv_icon)
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(X2C.inflate(context, R.layout.item_lib_reference, parent, false))
    }

    override fun convert(holder: BaseViewHolder, item: LibReference) {
        holder.setText(R.id.tv_lib_name, item.libName)

        item.chip?.let {
            holder.getView<ImageButton>(R.id.iv_icon).apply {
                load(it.iconRes)

                if (!GlobalValues.isColorfulIcon.valueUnsafe) {
                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                }
            }

            holder.setText(R.id.tv_label_name, it.name)
        } ?: let {
            holder.getView<ImageButton>(R.id.iv_icon).load(R.drawable.ic_question)
            val spannableString = SpannableString(context.getString(R.string.not_marked_lib))
            val colorSpanit = StyleSpan(Typeface.ITALIC)
            spannableString.setSpan(colorSpanit, 0, spannableString.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            holder.setText(R.id.tv_label_name, spannableString)
        }

        holder.setText(R.id.tv_count, item.referredCount.toString())
    }

}