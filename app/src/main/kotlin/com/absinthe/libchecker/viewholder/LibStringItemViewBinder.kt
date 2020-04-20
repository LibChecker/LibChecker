package com.absinthe.libchecker.viewholder

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.NativeLibMap
import com.drakeet.multitype.ItemViewBinder

class LibStringItemViewBinder :
    ItemViewBinder<LibStringItem, LibStringItemViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root: View = inflater.inflate(R.layout.item_lib_string, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: LibStringItem) {
        holder.apply {
            name.text = item.name
            NativeLibMap.MAP[item.name]?.let {
                libIcon.setImageResource(it)
            } ?: libIcon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.tv_name)
        var libIcon: ImageView = itemView.findViewById(R.id.iv_lib_icon)
    }
}