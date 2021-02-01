package com.absinthe.libchecker.recyclerview.adapter

import android.content.pm.ResolveInfo
import android.widget.ImageView
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.LCAppUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */
class AppInfoAdapter :BaseQuickAdapter<ResolveInfo, BaseViewHolder>(R.layout.layout_item_app_info) {

    override fun convert(holder: BaseViewHolder, item: ResolveInfo) {

        holder.getView<ImageView>(R.id.icon).apply {
            background = LCAppUtils.getAppIcon(item.activityInfo.packageName)
        }
        holder.setText(R.id.tv_name, item.activityInfo.loadLabel(LibCheckerApp.context.packageManager))
    }

}