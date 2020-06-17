package com.absinthe.libchecker.recyclerview

import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.SnapshotItem
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val ARROW = "→"

class SnapshotAdapter : BaseQuickAdapter<SnapshotItem, BaseViewHolder>(R.layout.item_snapshot) {

    override fun convert(holder: BaseViewHolder, item: SnapshotItem) {
        holder.setImageDrawable(
            R.id.iv_icon,
            PackageUtils.getPackageInfo(item.packageName).applicationInfo.loadIcon(context.packageManager)
        )
        holder.setText(R.id.tv_app_name, item.label)
        holder.setText(R.id.tv_package_name, item.packageName)
        holder.setText(R.id.tv_version, item.versionName)
    }
}