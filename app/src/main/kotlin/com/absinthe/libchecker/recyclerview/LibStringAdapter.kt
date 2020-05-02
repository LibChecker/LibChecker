package com.absinthe.libchecker.recyclerview

import android.text.format.Formatter
import android.view.View
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.NativeLibMap
import com.absinthe.libchecker.viewholder.LibStringItem
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.chip.Chip

class LibStringAdapter : BaseQuickAdapter<LibStringItem, BaseViewHolder>(R.layout.item_lib_string) {

    override fun convert(holder: BaseViewHolder, item: LibStringItem) {
        holder.setText(R.id.tv_name, item.name)
        if (item.size != 0L) {
            holder.setText(R.id.tv_lib_size, sizeToString(item.size))
        } else {
            holder.setText(R.id.tv_lib_size, "")
        }

        val libIcon = holder.getView<Chip>(R.id.chip)
        NativeLibMap.MAP[item.name]?.let {
            libIcon.apply {
                setChipIconResource(it.iconRes)
                text = it.name
                visibility = View.VISIBLE
            }
        } ?: let {
            libIcon.visibility = View.GONE
        }
    }

    private fun sizeToString(size: Long) : String {
        return "(${Formatter.formatFileSize(context, size)})"
    }
}