package com.absinthe.libchecker.recyclerview

import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.constant.NativeLibMap
import com.absinthe.libchecker.constant.ServiceLibMap
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LibReferenceAdapter : BaseQuickAdapter<LibReference, BaseViewHolder>(R.layout.item_lib_reference) {

    override fun convert(holder: BaseViewHolder, item: LibReference) {
        holder.setText(R.id.tv_lib_name, item.libName)
        NativeLibMap.MAP[item.libName]?.let {
            holder.setImageResource(R.id.iv_icon, it.iconRes)
            holder.setText(R.id.tv_label_name, it.name)
        } ?: let {
            ServiceLibMap.MAP[item.libName]?.let {
                holder.setImageResource(R.id.iv_icon, it.iconRes)
                holder.setText(R.id.tv_label_name, it.name)
            }
        } ?: let {
            holder.setText(R.id.tv_label_name, R.string.not_marked_lib)
        }
        holder.setText(R.id.tv_count, item.referredCount.toString())
    }

}