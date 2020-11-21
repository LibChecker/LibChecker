package com.absinthe.libchecker.recyclerview.adapter

import android.content.pm.PackageManager
import android.widget.ImageView
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.TrackListItem
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.switchmaterial.SwitchMaterial
import me.zhanghai.android.appiconloader.AppIconLoader

class TrackAdapter :BaseQuickAdapter<TrackListItem, BaseViewHolder>(R.layout.item_track) {

    private val iconLoader by lazy { AppIconLoader(context.resources.getDimensionPixelSize(R.dimen.app_icon_size), false,context) }

    init {
        addChildClickViewIds(R.id.track_switch)
    }

    override fun convert(holder: BaseViewHolder, item: TrackListItem) {
        val icon = try {
            iconLoader.loadIcon(PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_META_DATA).applicationInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        holder.getView<ImageView>(R.id.iv_icon).load(icon)
        holder.setText(R.id.tv_app_name, item.label)
        holder.setText(R.id.tv_package_name, item.packageName)
        holder.getView<SwitchMaterial>(R.id.track_switch).apply {
            isChecked = item.switchState
        }
    }

}