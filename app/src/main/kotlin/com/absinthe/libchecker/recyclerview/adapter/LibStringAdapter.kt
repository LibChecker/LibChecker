package com.absinthe.libchecker.recyclerview.adapter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.bean.DISABLED
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.extensions.valueUnsafe
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.chip.Chip
import com.zhangyue.we.x2c.X2C
import com.zhangyue.we.x2c.ano.Xml


@Xml(layouts = ["item_lib_string"])
class LibStringAdapter(@LibType val type: Int) : BaseQuickAdapter<LibStringItemChip, BaseViewHolder>(0) {

    init {
        addChildClickViewIds(R.id.chip)
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(X2C.inflate(context, R.layout.item_lib_string, parent, false))
    }

    override fun convert(holder: BaseViewHolder, item: LibStringItemChip) {
        val shouldHideSize = item.item.size == 0L && type != NATIVE

        if (item.item.source == DISABLED) {
            val sp = SpannableString(item.item.name)
            sp.setSpan(StrikethroughSpan(), 0, item.item.name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, item.item.name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.setText(R.id.tv_name, sp)
        } else {
            holder.setText(R.id.tv_name, item.item.name)
        }

        holder.setGone(R.id.tv_lib_size, shouldHideSize)

        if (!shouldHideSize) {
            holder.setText(R.id.tv_lib_size, PackageUtils.sizeToString(item.item))
        }

        val libIcon = holder.getView<Chip>(R.id.chip)

        item.chip?.let {
            libIcon.apply {
                setChipIconResource(it.iconRes)
                text = it.name
                visibility = View.VISIBLE

                if (!GlobalValues.isColorfulIcon.valueUnsafe) {
                    val icon = chipIcon
                    icon?.let {
                        it.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                        chipIcon = it
                    }
                }
            }
        } ?: let { libIcon.visibility = View.GONE }
    }
}