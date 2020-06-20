package com.absinthe.libchecker.recyclerview.diff

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.bean.AppItem

class AppListDiffUtil : DiffUtil.ItemCallback<AppItem>() {

    override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
        return oldItem.packageName == newItem.packageName
    }

    override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
        return oldItem.appName == newItem.appName
                && oldItem.abi == newItem.abi
                && oldItem.versionName == newItem.versionName
                && oldItem.updateTime == newItem.updateTime
    }
}