package com.absinthe.libchecker.recyclerview.adapter

import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppAdapter : BaseQuickAdapter<AppItem, BaseViewHolder>(R.layout.item_app) {

    override fun convert(holder: BaseViewHolder, item: AppItem) {
        holder.apply {
            setImageDrawable(R.id.iv_icon, item.icon)
            setText(R.id.tv_app_name, item.appName)
            setText(R.id.tv_package_name, item.packageName)
            setText(R.id.tv_version, item.versionName)
            setText(
                R.id.tv_abi, PackageUtils.getAbiString(item.abi))
            setImageResource(
                R.id.iv_abi_type, when (item.abi) {
                    ARMV8 -> R.drawable.ic_64bit
                    ARMV7, ARMV5 -> R.drawable.ic_32bit
                    else -> R.drawable.ic_no_libs
                }
            )
        }
    }
}