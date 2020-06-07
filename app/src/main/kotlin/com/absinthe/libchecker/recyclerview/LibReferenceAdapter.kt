package com.absinthe.libchecker.recyclerview

import android.graphics.PorterDuff
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.constant.GlobalValues
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class LibReferenceAdapter :
    BaseQuickAdapter<LibReference, BaseViewHolder>(R.layout.item_lib_reference) {

    override fun convert(holder: BaseViewHolder, item: LibReference) {
        holder.setText(R.id.tv_lib_name, item.libName)

        item.chip?.let {
            val icon = holder.getView<ImageView>(R.id.iv_icon)
            icon.setImageResource(it.iconRes)

            if (!GlobalValues.isColorfulIcon.value!!) {
                icon.setColorFilter(
                    ContextCompat.getColor(context, R.color.textNormal),
                    PorterDuff.Mode.SRC_IN
                )
            }

            holder.setText(R.id.tv_label_name, it.name)
        } ?: let {
            holder.setImageResource(R.id.iv_icon, R.drawable.ic_question)
            holder.setText(R.id.tv_label_name, R.string.not_marked_lib)
        }

        holder.setText(R.id.tv_count, item.referredCount.toString())
    }

}