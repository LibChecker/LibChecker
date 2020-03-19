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
        holder.abi.text = item.abi
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon: ImageView = itemView.findViewById(R.id.iv_icon)
        var appName: TextView = itemView.findViewById(R.id.tv_app_name)
        var abi: TextView = itemView.findViewById(R.id.tv_abi)
    }
}