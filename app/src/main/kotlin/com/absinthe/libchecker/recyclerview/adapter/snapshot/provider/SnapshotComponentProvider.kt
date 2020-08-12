package com.absinthe.libchecker.recyclerview.adapter.snapshot.provider

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.ADDED
import com.absinthe.libchecker.bean.CHANGED
import com.absinthe.libchecker.bean.MOVED
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.BaseMap
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            MOVED -> R.color.material_blue_300
            else -> Color.TRANSPARENT
        }

        helper.setImageResource(
            R.id.iv_type_icon,
            when (snapshotItem.diffType) {
                ADDED -> R.drawable.ic_add
                REMOVED -> R.drawable.ic_remove
                CHANGED -> R.drawable.ic_changed
                MOVED -> R.drawable.ic_move
                else -> Color.TRANSPARENT
            }
        )

        helper.itemView.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

        (context as BaseActivity).lifecycleScope.launch(Dispatchers.IO) {
            val chip = helper.getView<Chip>(R.id.chip)

            BaseMap.getMap(snapshotItem.itemType).getChip(snapshotItem.name)?.let {
                chip.apply {
                    withContext(Dispatchers.Main) {
                        setChipIconResource(it.iconRes)
                        text = it.name
                        chipBackgroundColor =
                            ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
                        visibility = View.VISIBLE

                        if (!GlobalValues.isColorfulIcon.value!!) {
                            chipIconTint = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    R.color.textNormal
                                )
                            )
                        }
                    }
                }
            } ?: withContext(Dispatchers.Main) { chip.isGone = true }
        }
    }
}