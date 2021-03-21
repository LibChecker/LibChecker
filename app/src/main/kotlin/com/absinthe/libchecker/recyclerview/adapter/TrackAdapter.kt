package com.absinthe.libchecker.recyclerview.adapter

import android.content.pm.PackageManager
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.TrackListItem
import com.absinthe.libchecker.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.AppIconCache
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.snapshot.TrackItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class TrackAdapter(val lifecycleScope: LifecycleCoroutineScope) : BaseQuickAdapter<TrackListItem, BaseViewHolder>(0) {

    private var loadIconJob: Job? = null

    init {
        addChildClickViewIds(android.R.id.toggle)
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(
            TrackItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
                    it.setMargins(margin, margin, margin, margin)
                }
            }
        )
    }

    override fun convert(holder: BaseViewHolder, item: TrackListItem) {
        (holder.itemView as TrackItemView).container.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val ai = PackageUtils.getPackageInfo(item.packageName).applicationInfo
                    val drawable = ai.loadIcon(context.packageManager)
                    icon.post { icon.setImageDrawable(drawable) }
                    loadIconJob = AppIconCache.loadIconBitmapAsync(context, ai, ai.uid / 100000, icon)
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.e(e)
                }
            }
            appName.text = item.label
            packageName.text = item.packageName
            switch.isChecked = item.switchState
        }
    }

    fun release() {
        if (loadIconJob?.isActive == true) {
            loadIconJob?.cancel()
        }
    }
}