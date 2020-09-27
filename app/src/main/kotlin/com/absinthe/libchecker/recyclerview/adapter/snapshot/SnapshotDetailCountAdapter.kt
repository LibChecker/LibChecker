package com.absinthe.libchecker.recyclerview.adapter.snapshot

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.ADDED
import com.absinthe.libchecker.bean.CHANGED
import com.absinthe.libchecker.bean.MOVED
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotDetailCountNode
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/**
 * <pre>
 * author : Absinthe
 * time : 2020/09/27
 * </pre>
 */
class SnapshotDetailCountAdapter : BaseQuickAdapter<SnapshotDetailCountNode, BaseViewHolder>(R.layout.item_snapshot_detail_count) {
    override fun convert(holder: BaseViewHolder, item: SnapshotDetailCountNode) {
        val colorRes = when (item.status) {
            ADDED -> R.color.material_green_200
            REMOVED -> R.color.material_red_200
            CHANGED -> R.color.material_yellow_200
            MOVED -> R.color.material_blue_200
            else -> Color.TRANSPARENT
        }

        holder.getView<TextView>(R.id.tv_count).apply {
            text = item.count.toString()
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
        }
    }
}