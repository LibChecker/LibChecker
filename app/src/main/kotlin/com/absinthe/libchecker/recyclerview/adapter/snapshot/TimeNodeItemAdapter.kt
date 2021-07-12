package com.absinthe.libchecker.recyclerview.adapter.snapshot

import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.absinthe.libchecker.utils.AppIconCache
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Job
import timber.log.Timber

class TimeNodeItemAdapter : BaseQuickAdapter<String, BaseViewHolder>(0) {

    private var loadIconJob: Job? = null

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(AppCompatImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(24.dp, 24.dp)
            setPadding(0, 0, 4.dp, 0)
        })
    }

    override fun convert(holder: BaseViewHolder, item: String) {
        try {
            val ai = PackageUtils.getPackageInfo(item).applicationInfo
            loadIconJob = AppIconCache.loadIconBitmapAsync(context, ai, ai.uid / 100000, (holder.itemView as AppCompatImageView))
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e)
        }
    }
}
