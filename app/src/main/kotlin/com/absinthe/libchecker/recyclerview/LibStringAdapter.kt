package com.absinthe.libchecker.recyclerview

import android.text.format.Formatter
import android.view.View
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.NativeLibMap
import com.absinthe.libchecker.constant.ServiceLibMap
import com.absinthe.libchecker.viewholder.LibStringItem
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.chip.Chip

const val MODE_NATIVE = 0
const val MODE_SERVICE = 1

class LibStringAdapter : BaseQuickAdapter<LibStringItem, BaseViewHolder>(R.layout.item_lib_string) {

    var mode = MODE_NATIVE

    override fun convert(holder: BaseViewHolder, item: LibStringItem) {
        holder.setText(R.id.tv_name, item.name)
        if (item.size != 0L) {
            holder.setText(R.id.tv_lib_size, sizeToString(item.size))
            holder.setGone(R.id.tv_lib_size, false)
        } else {
            holder.setGone(R.id.tv_lib_size, true)
        }

        val libIcon = holder.getView<Chip>(R.id.chip)

        val map = when (mode) {
            MODE_NATIVE -> NativeLibMap.MAP
            MODE_SERVICE -> ServiceLibMap.MAP
            else -> NativeLibMap.MAP
        }

        map[item.name]?.let {
            libIcon.apply {
                setChipIconResource(it.iconRes)
                text = it.name
                visibility = View.VISIBLE
            }
        } ?: let {
            libIcon.visibility = View.GONE
        }
    }

    private fun sizeToString(size: Long): String {
        return "(${Formatter.formatFileSize(context, size)})"
    }
}