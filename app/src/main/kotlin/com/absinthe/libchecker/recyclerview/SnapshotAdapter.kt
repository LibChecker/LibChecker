package com.absinthe.libchecker.recyclerview

import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val ARROW = "→"

class SnapshotAdapter : BaseQuickAdapter<SnapshotDiffItem, BaseViewHolder>(R.layout.item_snapshot) {

    override fun convert(holder: BaseViewHolder, item: SnapshotDiffItem) {
        holder.setImageDrawable(
            R.id.iv_icon,
            PackageUtils.getPackageInfo(item.packageName).applicationInfo.loadIcon(context.packageManager)
        )

        if (item.added) {
            holder.getView<TextView>(R.id.indicator_added).isVisible = true
        }
        if (item.removed) {
            holder.getView<TextView>(R.id.indicator_removed).isVisible = true
        }
        if (item.changed) {
            holder.getView<TextView>(R.id.indicator_changed).isVisible = true
        }

        if (item.labelDiff.old != item.labelDiff.new) {
            holder.setText(R.id.tv_app_name, "${item.labelDiff.old} $ARROW ${item.labelDiff.new}")
        } else {
            holder.setText(R.id.tv_app_name, item.labelDiff.old)
        }
        holder.setText(R.id.tv_package_name, item.packageName)

        if (item.versionNameDiff.old != item.versionNameDiff.new || item.versionCodeDiff.old != item.versionCodeDiff.new) {
            holder.setText(
                R.id.tv_version,
                "${item.versionNameDiff.old} (${item.versionCodeDiff.old}) $ARROW ${item.versionNameDiff.new ?: item.versionNameDiff.old} (${item.versionCodeDiff.new ?: item.versionCodeDiff.old})"
            )
        } else {
            holder.setText(
                R.id.tv_version,
                "${item.versionNameDiff.old} (${item.versionCodeDiff.old})"
            )
        }
        if (item.targetApiDiff.old != item.targetApiDiff.new) {
            holder.setText(R.id.tv_target_api, "API ${item.targetApiDiff.old} $ARROW API ${item.targetApiDiff.new}")
        } else {
            holder.setText(R.id.tv_target_api, item.targetApiDiff.old.toString())
        }

        holder.setText(
            R.id.tv_abi, when (item.abiDiff.old.toInt()) {
                ARMV8 -> ARMV8_STRING
                ARMV7 -> ARMV7_STRING
                ARMV5 -> ARMV5_STRING
                NO_LIBS -> context.getText(R.string.no_libs)
                ERROR -> "Can\'t read"
                else -> "Unknown"
            }
        )
        holder.setImageResource(
            R.id.iv_abi_type, when (item.abiDiff.old.toInt()) {
                ARMV8 -> R.drawable.ic_64bit
                ARMV7, ARMV5 -> R.drawable.ic_32bit
                else -> R.drawable.ic_no_libs
            }
        )
        if (item.abiDiff.new != null && item.abiDiff.old != item.abiDiff.new) {
            holder.getView<TextView>(R.id.tv_arrow).isVisible = true

            val abiBadgeNewLayout = holder.getView<LinearLayout>(R.id.layout_abi_badge_new)
            abiBadgeNewLayout.isVisible = true
            abiBadgeNewLayout.findViewById<TextView>(R.id.tv_abi).text =
                when (item.abiDiff.new.toInt()) {
                    ARMV8 -> ARMV8_STRING
                    ARMV7 -> ARMV7_STRING
                    ARMV5 -> ARMV5_STRING
                    NO_LIBS -> context.getText(R.string.no_libs)
                    ERROR -> "Can\'t read"
                    else -> "Unknown"
                }
            abiBadgeNewLayout.findViewById<ImageView>(R.id.iv_abi_type).setImageResource(
                when (item.abiDiff.new.toInt()) {
                    ARMV8 -> R.drawable.ic_64bit
                    ARMV7, ARMV5 -> R.drawable.ic_32bit
                    else -> R.drawable.ic_no_libs
                }
            )
        }
    }
}