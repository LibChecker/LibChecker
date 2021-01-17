package com.absinthe.libchecker.recyclerview.adapter

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.PackageUtils
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

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return createBaseViewHolder(X2C.inflate(context, R.layout.item_app, parent, false))
    }

    override fun convert(holder: BaseViewHolder, item: LCItem) {
        holder.apply {
            getView<ImageView>(R.id.iv_icon).apply {
                tag = item.packageName
                iconMap[item.packageName]?.let {
                    load(it) {
                        crossfade(true)
                    }
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

            setText(R.id.tv_app_name, item.label)
            setText(R.id.tv_package_name, item.packageName)
            setText(R.id.tv_version, PackageUtils.getVersionString(item.versionName, item.versionCode))
            setText(R.id.tv_abi, PackageUtils.getAbiString(item.abi.toInt()))
            getView<ImageView>(R.id.iv_abi_type).load(PackageUtils.getAbiBadgeResource(item.abi.toInt()))
            itemView.transitionName = item.packageName
        }
    }

    fun release() {
        iconMap.forEach { it.value.recycle() }
    }
}