package com.absinthe.libchecker.recyclerview.adapter

import android.content.pm.PackageManager
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.AppIconCache
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.absinthe.libchecker.view.applist.AppItemView
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class AppAdapter(val lifecycleScope: LifecycleCoroutineScope) : BaseQuickAdapter<LCItem, BaseViewHolder>(0) {

    private var loadIconJob: Job? = null
    var highlightText: String = ""

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(
            AppItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                    val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
                    it.setMargins(margin, margin, margin, margin)
                }
            }
        )
    }

    override fun convert(holder: BaseViewHolder, item: LCItem) {
        (holder.itemView as AppItemView).container.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val ai = PackageUtils.getPackageInfo(item.packageName).applicationInfo
                    loadIconJob = AppIconCache.loadIconBitmapAsync(context, ai, ai.uid / 100000, icon)
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.e(e)
                }
            }

            if (highlightText.isNotBlank()) {
                appName.tintHighlightText(highlightText, item.label)
            } else {
                appName.text = item.label
            }
            if (highlightText.isNotBlank()) {
                packageName.tintHighlightText(highlightText, item.packageName)
            } else {
                packageName.text = item.packageName
            }

            versionInfo.text = PackageUtils.getVersionString(item.versionName, item.versionCode)

            val str = StringBuilder()
                .append(PackageUtils.getAbiString(context, item.abi.toInt(), true))
                .append(", ")
                .append(PackageUtils.getTargetApiString(item.targetApi))
            val spanString: SpannableString
            val abiBadgeRes = PackageUtils.getAbiBadgeResource(item.abi.toInt())

            if (item.abi.toInt() != Constants.OVERLAY && item.abi.toInt() != Constants.ERROR && abiBadgeRes != 0) {
                spanString = SpannableString("  $str")
                ContextCompat.getDrawable(context, abiBadgeRes)?.let {
                    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                    val span = CenterAlignImageSpan(it)
                    spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
                }
                abiInfo.text = spanString
            } else {
                abiInfo.text = str
            }

            if (item.variant == Constants.VARIANT_HAP) {
                setBadge(R.drawable.ic_harmony_badge)
            } else {
                setBadge(null)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return data[position].hashCode().toLong()
    }

    fun release() {
        if (loadIconJob?.isActive == true) {
            loadIconJob?.cancel()
        }
    }
}
