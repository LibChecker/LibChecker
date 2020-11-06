package com.absinthe.libchecker.recyclerview.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.PackageUtils
import com.blankj.utilcode.util.AppUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.zhangyue.we.x2c.X2C
import com.zhangyue.we.x2c.ano.Xml

@Xml(layouts = ["item_app"])
class AppAdapter : BaseQuickAdapter<LCItem, BaseViewHolder>(0) {
    
    val iconMap = hashMapOf<String, Drawable>()

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(X2C.inflate(context, R.layout.item_app, parent, false))
    }

    override fun convert(holder: BaseViewHolder, item: LCItem) {
        holder.apply {
            iconMap[item.packageName]?.let {
                setImageDrawable(R.id.iv_icon, it)
            } ?: run {
                val drawable = AppUtils.getAppIcon(item.packageName) ?: ColorDrawable(Color.TRANSPARENT)
                setImageDrawable(R.id.iv_icon, drawable)
                iconMap[item.packageName] = drawable
            }
            setText(R.id.tv_app_name, item.label)
            setText(R.id.tv_package_name, item.packageName)
            setText(R.id.tv_version, PackageUtils.getVersionString(item.versionName, item.versionCode))
            setText(R.id.tv_abi, PackageUtils.getAbiString(item.abi.toInt()))
            getView<ImageView>(R.id.iv_abi_type).load(PackageUtils.getAbiBadgeResource(item.abi.toInt()))
            itemView.transitionName = item.packageName
        }
    }
}