package com.absinthe.libchecker.recyclerview.adapter.snapshot.provider

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.ADDED
import com.absinthe.libchecker.bean.CHANGED
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotNativeNode
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val SNAPSHOT_COMPONENT_PROVIDER = 3

class SnapshotComponentProvider : BaseNodeProvider() {

    override val itemViewType: Int = SNAPSHOT_COMPONENT_PROVIDER

    override val layoutId: Int = R.layout.item_snapshot_detail_component

    override fun convert(helper: BaseViewHolder, item: BaseNode) {
        val snapshotItem = (item as SnapshotComponentNode).item

        helper.setText(R.id.tv_name, snapshotItem.title)

        val colorRes = when (snapshotItem.diffType) {
            ADDED -> R.color.material_green_300
            REMOVED -> R.color.material_red_300
            CHANGED -> R.color.material_yellow_300
            else -> 0
        }

        helper.setImageResource(R.id.iv_type_icon,
            when (snapshotItem.diffType) {
                ADDED -> R.drawable.ic_add
                REMOVED -> R.drawable.ic_remove
                CHANGED -> R.drawable.ic_changed
                else -> 0
            }
        )

        helper.itemView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
    }
}