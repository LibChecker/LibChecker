package com.absinthe.libchecker.recyclerview

import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.constant.librarymap.*
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LibReferenceAdapter :
    BaseQuickAdapter<LibReference, BaseViewHolder>(R.layout.item_lib_reference) {

    override fun convert(holder: BaseViewHolder, item: LibReference) {
        holder.setText(R.id.tv_lib_name, item.libName)

        var found = false
        val map: BaseMap? = when (item.type) {
            LibReferenceActivity.Type.TYPE_NATIVE -> NativeLibMap
            LibReferenceActivity.Type.TYPE_SERVICE -> ServiceLibMap
            LibReferenceActivity.Type.TYPE_ACTIVITY -> ActivityLibMap
            LibReferenceActivity.Type.TYPE_BROADCAST_RECEIVER -> ReceiverLibMap
            LibReferenceActivity.Type.TYPE_CONTENT_PROVIDER -> ProviderLibMap
            else -> null
        }
        map?.apply {
            getMap()[item.libName]?.let {
                holder.setImageResource(R.id.iv_icon, it.iconRes)
                holder.setText(R.id.tv_label_name, it.name)
                found = true
            }?:let {
                findRegex(item.libName)?.let {
                    holder.setImageResource(R.id.iv_icon, it.iconRes)
                    holder.setText(R.id.tv_label_name, it.name)
                    found = true
                }
            }
        } ?: apply {
            holder.setImageResource(R.id.iv_icon, R.drawable.ic_question)
            holder.setText(R.id.tv_label_name, R.string.not_marked_lib)
        }

        if (!found) {
            holder.setImageResource(R.id.iv_icon, R.drawable.ic_question)
            holder.setText(R.id.tv_label_name, R.string.not_marked_lib)
        }

        holder.setText(R.id.tv_count, item.referredCount.toString())
    }

}