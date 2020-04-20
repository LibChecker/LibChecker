package com.absinthe.libchecker.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.MainActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.NativeLibDialogFragment
import com.drakeet.multitype.ItemViewBinder

class AppItemViewBinder : ItemViewBinder<AppItem, AppItemViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root: View = inflater.inflate(R.layout.item_app, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: AppItem) {
        holder.apply {
            icon.setImageDrawable(item.icon)
            appName.text = item.appName
            packageName.text = item.packageName
            versionName.text = item.versionName
            abi.text = when (item.abi) {
                ARMV8 -> ARMV8_STRING
                ARMV7 -> ARMV7_STRING
                ARMV5 -> ARMV5_STRING
                else -> holder.itemView.context.getText(R.string.no_libs)
            }
            abiType.setImageResource(
                when (item.abi) {
                    ARMV8 -> R.drawable.ic_64bit
                    ARMV7 -> R.drawable.ic_32bit
                    ARMV5 -> R.drawable.ic_32bit
                    else -> 0
                }
            )

            itemView.setOnClickListener {
                MainActivity.instance?.let { instance ->
                    NativeLibDialogFragment(item.packageName).show(
                        instance.supportFragmentManager,
                        "NativeLibDialogFragment"
                    )
                }
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon: ImageView = itemView.findViewById(R.id.iv_icon)
        var appName: TextView = itemView.findViewById(R.id.tv_app_name)
        var packageName: TextView = itemView.findViewById(R.id.tv_package_name)
        var versionName: TextView = itemView.findViewById(R.id.tv_version)
        var abi: TextView = itemView.findViewById(R.id.tv_abi)
        var abiType: ImageView = itemView.findViewById(R.id.iv_abi_type)
    }
}