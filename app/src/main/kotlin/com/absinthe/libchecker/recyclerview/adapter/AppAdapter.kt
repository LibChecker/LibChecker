package com.absinthe.libchecker.recyclerview.adapter

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.zhangyue.we.x2c.X2C
import com.zhangyue.we.x2c.ano.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.appiconloader.AppIconLoader

@Xml(layouts = ["item_app"])
class AppAdapter : BaseQuickAdapter<LCItem, BaseViewHolder>(0) {

    private val iconLoader by lazy { AppIconLoader(context.resources.getDimensionPixelSize(R.dimen.app_icon_size), false, context) }
    private val iconMap = mutableMapOf<String, Bitmap>()
    var hightlightText: String = ""

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(X2C.inflate(context, R.layout.item_app, parent, false))
    }

    override fun convert(holder: BaseViewHolder, item: LCItem) {
        holder.apply {
            getView<ImageView>(R.id.iv_icon).apply {
                tag = item.packageName
                iconMap[item.packageName]?.let {
                    load(it)
                } ?: let {
                    GlobalScope.launch(Dispatchers.IO) {
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

            if (hightlightText.isNotBlank() && item.label.contains(hightlightText, true)) {
                val builder = SpannableStringBuilder()
                val spannableString = SpannableString(item.label)
                val start = item.label.indexOf(hightlightText, 0, true)
                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary)),
                    start, start + hightlightText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
                builder.append(spannableString)
                setText(R.id.tv_app_name, builder)
            } else {
                setText(R.id.tv_app_name, item.label)
            }
            if (hightlightText.isNotBlank() && item.packageName.contains(hightlightText, true)) {
                val builder = SpannableStringBuilder()
                val spannableString = SpannableString(item.packageName)
                val start = item.packageName.indexOf(hightlightText, 0, true)
                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorPrimary)),
                    start, start + hightlightText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
                builder.append(spannableString)
                setText(R.id.tv_package_name, builder)
            } else {
                setText(R.id.tv_package_name, item.packageName)
            }

            setText(R.id.tv_version, PackageUtils.getVersionString(item.versionName, item.versionCode))

            val spanString = SpannableString("  ${PackageUtils.getAbiString(item.abi.toInt())}, ${PackageUtils.getTargetApiString(item.packageName)}")
            ContextCompat.getDrawable(context, PackageUtils.getAbiBadgeResource(item.abi.toInt()))?.let {
                it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                val span = CenterAlignImageSpan(it)
                spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
            }

            setText(R.id.tv_abi_and_api, spanString)
            itemView.transitionName = item.packageName
        }
    }

    override fun getItemId(position: Int): Long {
        return data[position].hashCode().toLong()
    }

    fun release() {
        iconMap.forEach { it.value.recycle() }
    }
}