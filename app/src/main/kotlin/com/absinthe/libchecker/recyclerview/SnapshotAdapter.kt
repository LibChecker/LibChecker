package com.absinthe.libchecker.recyclerview

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

const val ARROW = "→"

class SnapshotAdapter : BaseQuickAdapter<SnapshotDiffItem, BaseViewHolder>(R.layout.item_snapshot) {

    private val gson = Gson()

    override fun convert(holder: BaseViewHolder, item: SnapshotDiffItem) {
        try {
            holder.setImageDrawable(
                R.id.iv_icon,
                PackageUtils.getPackageInfo(item.packageName).applicationInfo.loadIcon(context.packageManager)
            )
        } catch (e: Exception) {
            holder.setImageDrawable(R.id.iv_icon, null)
        }

        var isNewOrDeleted = false
        when {
            item.deleted -> {
                holder.itemView.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.material_red_300))
                holder.setTextColor(R.id.tv_version, ContextCompat.getColor(context, R.color.textNormal))
                holder.setTextColor(R.id.tv_target_api, ContextCompat.getColor(context, R.color.textNormal))
                isNewOrDeleted = true
            }
            item.newInstalled -> {
                holder.itemView.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.material_green_300))
                holder.setTextColor(R.id.tv_version, ContextCompat.getColor(context, R.color.textNormal))
                holder.setTextColor(R.id.tv_target_api, ContextCompat.getColor(context, R.color.textNormal))
                isNewOrDeleted = true
            }
            else -> {
                holder.itemView.backgroundTintList = null
                holder.setTextColor(R.id.tv_version, ContextCompat.getColor(context, android.R.color.darker_gray))
                holder.setTextColor(R.id.tv_target_api, ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }

        if (!isNewOrDeleted) {
            val compareNode = compareNativeAndComponentDiff(item)
            if (compareNode.added) {
                holder.getView<TextView>(R.id.indicator_added).isVisible = true
            }
            if (compareNode.removed) {
                holder.getView<TextView>(R.id.indicator_removed).isVisible = true
            }
            if (compareNode.changed) {
                holder.getView<TextView>(R.id.indicator_changed).isVisible = true
            }
        }

        if (item.labelDiff.old != item.labelDiff.new && !isNewOrDeleted) {
            holder.setText(R.id.tv_app_name, "${item.labelDiff.old} $ARROW ${item.labelDiff.new}")
        } else {
            holder.setText(R.id.tv_app_name, item.labelDiff.old)
        }
        holder.setText(R.id.tv_package_name, item.packageName)

        if ((item.versionNameDiff.old != item.versionNameDiff.new || item.versionCodeDiff.old != item.versionCodeDiff.new) && !isNewOrDeleted) {
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
        if (item.targetApiDiff.old != item.targetApiDiff.new && !isNewOrDeleted) {
            holder.setText(
                R.id.tv_target_api,
                "API ${item.targetApiDiff.old} $ARROW API ${item.targetApiDiff.new}"
            )
        } else {
            holder.setText(R.id.tv_target_api, "API ${item.targetApiDiff.old}")
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

    private fun compareNativeAndComponentDiff(item: SnapshotDiffItem): CompareDiffNode {
        val nativeCompareNode = compareNativeDiff(
            gson.fromJson(
                item.nativeLibsDiff.old,
                object : TypeToken<List<LibStringItem>>() {}.type
            ),
            gson.fromJson(
                item.nativeLibsDiff.new,
                object : TypeToken<List<LibStringItem>>() {}.type
            )
        )
        val servicesCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.servicesDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.servicesDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val activitiesCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.activitiesDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.activitiesDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val receiversCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.receiversDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.receiversDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val providersCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.providersDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.providersDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )

        val totalNode = CompareDiffNode()
        totalNode.added =
            nativeCompareNode.added or servicesCompareNode.added or activitiesCompareNode.added or receiversCompareNode.added or providersCompareNode.added
        totalNode.removed =
            nativeCompareNode.removed or servicesCompareNode.removed or activitiesCompareNode.removed or receiversCompareNode.removed or providersCompareNode.removed
        totalNode.changed =
            nativeCompareNode.changed or servicesCompareNode.changed or activitiesCompareNode.changed or receiversCompareNode.changed or providersCompareNode.changed

        return totalNode
    }

    private fun compareNativeDiff(
        oldList: List<LibStringItem>,
        newList: List<LibStringItem>?
    ): CompareDiffNode {
        if (newList == null) {
            return CompareDiffNode(removed = true)
        }

        val tempOldList = oldList.toMutableList()
        val tempNewList = newList.toMutableList()
        val sameList = mutableListOf<LibStringItem>()
        val node = CompareDiffNode()

        for (item in tempNewList) {
            oldList.find { it.name == item.name }?.let {
                if (it.size != item.size) {
                    node.changed = true
                }
                sameList.add(item)
            }
        }

        for (item in sameList) {
            tempOldList.remove(item)
            tempNewList.remove(item)
        }

        if (tempOldList.isNotEmpty()) {
            node.removed = true
        }
        if (tempNewList.isNotEmpty()) {
            node.added = true
        }
        return node
    }

    private fun compareComponentsDiff(
        oldList: List<String>,
        newList: List<String>?
    ): CompareDiffNode {
        if (newList == null) {
            return CompareDiffNode(removed = true)
        }

        val tempOldList = oldList.toMutableList()
        val tempNewList = newList.toMutableList()
        val sameList = mutableListOf<String>()
        val node = CompareDiffNode()

        for (item in tempNewList) {
            oldList.find { it == item }?.let {
                sameList.add(item)
            }
        }

        for (item in sameList) {
            tempOldList.remove(item)
            tempNewList.remove(item)
        }

        if (tempOldList.isNotEmpty()) {
            node.removed = true
        }
        if (tempNewList.isNotEmpty()) {
            node.added = true
        }
        return node
    }

    data class CompareDiffNode(
        var added: Boolean = false,
        var removed: Boolean = false,
        var changed: Boolean = false
    )
}