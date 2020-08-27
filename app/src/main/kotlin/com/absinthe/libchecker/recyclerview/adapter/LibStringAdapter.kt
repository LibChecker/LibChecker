package com.absinthe.libchecker.recyclerview.adapter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibStringItemChip
import com.absinthe.libchecker.constant.DEX
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LibType
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
        holder.setText(R.id.tv_name, item.item.name)
        holder.setGone(R.id.tv_lib_size, item.item.size == 0L)

        if (item.item.size != 0L) {
            val text = if (type == DEX) {
                "${item.item.size} files"
            } else {
                PackageUtils.sizeToString(item.item.size)
            }
            holder.setText(R.id.tv_lib_size, text)
        }

        val libIcon = holder.getView<Chip>(R.id.chip)

        item.chip?.let {
            libIcon.apply {
                setChipIconResource(it.iconRes)
                text = it.name
                visibility = View.VISIBLE

                if (!GlobalValues.isColorfulIcon.valueUnsafe) {
                    chipDrawable.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                }
            }
        } ?: let { libIcon.visibility = View.GONE }
    }
}