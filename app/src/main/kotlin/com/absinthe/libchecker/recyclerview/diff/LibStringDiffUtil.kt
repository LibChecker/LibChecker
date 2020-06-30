package com.absinthe.libchecker.recyclerview.diff

import androidx.recyclerview.widget.DiffUtil
import com.absinthe.libchecker.bean.LibStringItem

class LibStringDiffUtil : DiffUtil.ItemCallback<LibStringItem>() {

    override fun areItemsTheSame(oldItem: LibStringItem, newItem: LibStringItem): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: LibStringItem, newItem: LibStringItem): Boolean {
        return oldItem.name == newItem.name
                && oldItem.size == newItem.size
    }
}