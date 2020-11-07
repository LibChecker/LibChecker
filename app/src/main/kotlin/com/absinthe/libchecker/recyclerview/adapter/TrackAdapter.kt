package com.absinthe.libchecker.recyclerview.adapter

import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.TrackListItem
import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.entity.TrackItem
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackAdapter(private val repository: LCRepository) :BaseQuickAdapter<TrackListItem, BaseViewHolder>(R.layout.item_track) {

    init {
        addChildClickViewIds(R.id.track_switch)
    }

    override fun convert(holder: BaseViewHolder, item: TrackListItem) {
        holder.setImageDrawable(R.id.iv_icon, item.icon)
        holder.setText(R.id.tv_app_name, item.label)
        holder.setText(R.id.tv_package_name, item.packageName)
        holder.getView<SwitchMaterial>(R.id.track_switch).apply {
            isChecked = item.switchState
            setOnCheckedChangeListener { _, isChecked ->
                    (context as BaseActivity).lifecycleScope.launch(Dispatchers.IO) {
                    if (isChecked) {
                        repository.insert(TrackItem(item.packageName))
                    } else {
                        repository.delete(TrackItem(item.packageName))
                    }
                    withContext(Dispatchers.Main) {
                        item.switchState = isChecked
                    }
                }
            }
        }
    }

}