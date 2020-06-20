package com.absinthe.libchecker.recyclerview.diff

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.bean.LibReference

class RefListDiffUtil : DiffUtil.ItemCallback<LibReference>() {

    override fun areItemsTheSame(oldItem: LibReference, newItem: LibReference): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }

    override fun areContentsTheSame(oldItem: LibReference, newItem: LibReference): Boolean {
        return oldItem.libName == newItem.libName
                && oldItem.referredCount == newItem.referredCount
                && oldItem.chip == newItem.chip
                && oldItem.type == newItem.type
    }
}