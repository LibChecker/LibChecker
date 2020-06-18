package com.absinthe.libchecker.recyclerview

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.*
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class SnapshotDetailAdapter : BaseMultiItemQuickAdapter<SnapshotDetailItem, BaseViewHolder>() {

    init {
        addItemType(TYPE_NATIVE, R.layout.item_snapshot_detail_native)
        addItemType(TYPE_COMPONENT, R.layout.item_snapshot_detail_component)
    }

    override fun convert(holder: BaseViewHolder, item: SnapshotDetailItem) {
        holder.setText(R.id.tv_name, item.title)

        if (holder.itemViewType == TYPE_NATIVE) {
            holder.setText(R.id.tv_lib_size, item.extra)
        }

        val colorRes = when (item.diffType) {
            ADDED -> R.color.material_green_300
            REMOVED -> R.color.material_red_300
            CHANGED -> R.color.material_yellow_300
            else -> 0
        }
        holder.itemView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
    }

}