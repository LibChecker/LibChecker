package com.absinthe.libchecker.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.drakeet.multitype.ItemViewBinder

class AppItemViewBinder : ItemViewBinder<AppItem, AppItemViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root: View = inflater.inflate(R.layout.item_app, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppItem) {
        holder.icon.setImageDrawable(item.icon)
        holder.appName.text = item.appName
        holder.abi.text = when(item.abi) {
            ARMV8 -> ARMV8_STRING
            ARMV7 -> ARMV7_STRING
            ARMV5 -> ARMV5_STRING
            else -> NO_LIBS_STRING
        }
        holder.abiType.setImageResource(when(item.abi) {
            ARMV8 -> R.drawable.ic_64bit
            ARMV7 -> R.drawable.ic_32bit
            ARMV5 -> R.drawable.ic_32bit
            else -> 0
        })
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon: ImageView = itemView.findViewById(R.id.iv_icon)
        var appName: TextView = itemView.findViewById(R.id.tv_app_name)
        var abi: TextView = itemView.findViewById(R.id.tv_abi)
        var abiType: ImageView = itemView.findViewById(R.id.iv_abi_type)
    }
}