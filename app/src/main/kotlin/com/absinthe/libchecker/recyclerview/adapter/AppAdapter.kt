package com.absinthe.libchecker.recyclerview.adapter

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleCoroutineScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.extensions.getDimensionPixelSize
import com.absinthe.libchecker.extensions.tintHighlightText
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.applist.AppItemView
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader

class AppAdapter(val lifecycleScope: LifecycleCoroutineScope) : BaseQuickAdapter<LCItem, BaseViewHolder>(0) {

    private val iconLoader by lazy { AppIconLoader(context.resources.getDimensionPixelSize(R.dimen.app_icon_size), false, context) }
    private val iconMap = mutableMapOf<String, Bitmap>()
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
            icon.apply {
                tag = item.packageName
                iconMap[item.packageName]?.let {
                    load(it)
                } ?: let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        var applicationInfo: ApplicationInfo? = null
                        val bitmap = try {
                            applicationInfo = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_META_DATA).applicationInfo
                            iconLoader.loadIcon(applicationInfo)
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        } catch (e: SecurityException) {
                            applicationInfo?.loadIcon(context.packageManager)?.toBitmap()
                        }
                        withContext(Dispatchers.Main) {
                            findViewWithTag<ImageView>(item.packageName)?.let {
                                load(bitmap) {
                                    crossfade(true)
                                }
                            }
                        }
                        if (bitmap != null) {
                            iconMap[item.packageName] = bitmap
                        }
                    }
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

            val spanString = SpannableString("  ${PackageUtils.getAbiString(context, item.abi.toInt(), true)}, ${PackageUtils.getTargetApiString(item.targetApi)}")
            ContextCompat.getDrawable(context, PackageUtils.getAbiBadgeResource(item.abi.toInt()))?.let {
                it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                val span = CenterAlignImageSpan(it)
                spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
            }

            abiInfo.text = spanString
        }
    }

    override fun getItemId(position: Int): Long {
        return data[position].hashCode().toLong()
    }

    fun release() {
        iconMap.forEach { it.value.recycle() }
    }
}