package com.absinthe.libchecker.recyclerview

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.viewholder.AppItem

class AppListDiffUtil(private val oldList: List<AppItem>, private val newList: List<AppItem>) :
    DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].packageName == newList[newItemPosition].packageName
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return oldItem.appName == newItem.appName
                && oldItem.abi == newItem.abi
                && oldItem.versionName == newItem.versionName
                && oldItem.updateTime == newItem.updateTime
    }

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }
}