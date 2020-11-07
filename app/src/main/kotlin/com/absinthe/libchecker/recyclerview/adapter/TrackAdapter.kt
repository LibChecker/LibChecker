package com.absinthe.libchecker.recyclerview.adapter

import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.TrackListItem
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.switchmaterial.SwitchMaterial

class TrackAdapter() :BaseQuickAdapter<TrackListItem, BaseViewHolder>(R.layout.item_track) {

    init {
        addChildClickViewIds(R.id.track_switch)
    }

    override fun convert(holder: BaseViewHolder, item: TrackListItem) {
        holder.setImageDrawable(R.id.iv_icon, item.icon)
        holder.setText(R.id.tv_app_name, item.label)
        holder.setText(R.id.tv_package_name, item.packageName)
        holder.getView<SwitchMaterial>(R.id.track_switch).apply {
            isChecked = item.switchState
        }
    }

}