package com.absinthe.libchecker.recyclerview

import com.absinthe.libchecker.R
import com.absinthe.libchecker.viewholder.*
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
                R.id.tv_abi, when (item.abi) {
                    ARMV8 -> ARMV8_STRING
                    ARMV7 -> ARMV7_STRING
                    ARMV5 -> ARMV5_STRING
                    NO_LIBS -> context.getText(R.string.no_libs)
                    ERROR -> "Can\'t read"
                    else -> "Can\'t read"
                }
            )
            setImageResource(
                R.id.iv_abi_type, when (item.abi) {
                    ARMV8 -> R.drawable.ic_64bit
                    ARMV7 -> R.drawable.ic_32bit
                    ARMV5 -> R.drawable.ic_32bit
                    else -> 0
                }
            )
        }
    }
}