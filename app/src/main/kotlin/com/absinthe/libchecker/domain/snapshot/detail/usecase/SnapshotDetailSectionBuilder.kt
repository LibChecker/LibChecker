package com.absinthe.libchecker.domain.snapshot.detail.usecase

import android.content.Context
import android.graphics.Color
import android.text.style.ForegroundColorSpan
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailStatusCount
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailItemBackgroundColor
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailItemDescription
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportItemText
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailSectionDescription
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libraries.utils.utils.UiUtils
import com.absinthe.rulesbundle.Rule
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SnapshotDetailSectionBuilder(
  private val context: Context,
  private val appListSettingsRepository: AppListSettingsRepository
) {

  suspend operator fun invoke(item: SnapshotDiffItem): List<SnapshotDetailSection> = withContext(Dispatchers.IO) {
    val list = mutableListOf<SnapshotDetailItem>()

    list.addAll(
      getNativeDiffList(
        item.nativeLibsDiff.old.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        ) ?: emptyList(),
        item.nativeLibsDiff.new?.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        )
      )
    )
    addComponentDiffInfoFromJson(list, item.servicesDiff, SERVICE)
    addComponentDiffInfoFromJson(list, item.activitiesDiff, ACTIVITY)
    addComponentDiffInfoFromJson(list, item.receiversDiff, RECEIVER)
    addComponentDiffInfoFromJson(list, item.providersDiff, PROVIDER)

    list.addAll(
      getPermissionsDiffList(
        item.permissionsDiff.old.fromJson<List<String>>(
          List::class.java,
          String::class.java
        ).orEmpty().toSet(),
        item.permissionsDiff.new?.fromJson<List<String>>(
          List::class.java,
          String::class.java
        )?.toSet()
      )
    )

    list.addAll(
      getMetadataDiffList(
        item.metadataDiff.old.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        ) ?: emptyList(),
        item.metadataDiff.new?.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        )
      )
    )

    buildSections(list)
  }

  private suspend fun buildSections(items: List<SnapshotDetailItem>): List<SnapshotDetailSection> {
    val colorfulRuleIcon = appListSettingsRepository.colorfulRuleIcon
    val darkMode = UiUtils.isDarkMode()
    val ruleCache = mutableMapOf<String, Rule?>()

    suspend fun getRuleCached(item: SnapshotDetailItem): Rule? {
      val key = "${item.itemType}:${item.name}"
      if (ruleCache.containsKey(key)) {
        return ruleCache[key]
      }
      return getRule(item).also {
        ruleCache[key] = it
      }
    }

    return orderedTypes.mapNotNull { type ->
      val sectionItems = items
        .filter { it.itemType == type }
        .map { item ->
          val status = buildStatusDisplayData(item.diffType)
          val rule = getRuleCached(item)
          val ruleChip = buildSnapshotDetailRuleChipDisplayData(rule, colorfulRuleIcon)
          SnapshotDetailItemDisplayData(
            item = item,
            title = item.title,
            extra = item.extra,
            description = buildSnapshotDetailItemDescription(
              statusLabel = context.getString(status.labelRes),
              title = item.title,
              extra = item.extra,
              ruleLabel = ruleChip?.label
            ),
            reportText = buildSnapshotDetailReportItemText(item),
            status = status,
            backgroundColor = buildSnapshotDetailItemBackgroundColor(
              baseColor = status.colorRes.getColor(context),
              darkMode = darkMode
            ),
            ruleChip = ruleChip
          )
        }
      if (sectionItems.isEmpty()) {
        null
      } else {
        val statusCounts = buildStatusCounts(sectionItems)
        val title = context.getString(getSectionTitleRes(type))
        SnapshotDetailSection(
          type = type,
          title = title,
          expandedDescription = buildSnapshotDetailSectionDescription(
            title = title,
            statusCounts = statusCounts,
            expansionStateLabel = context.getString(R.string.a11y_state_expanded)
          ),
          collapsedDescription = buildSnapshotDetailSectionDescription(
            title = title,
            statusCounts = statusCounts,
            expansionStateLabel = context.getString(R.string.a11y_state_collapsed)
          ),
          items = sectionItems,
          statusCounts = statusCounts
        )
      }
    }
  }

  private fun buildStatusCounts(items: List<SnapshotDetailItemDisplayData>): List<SnapshotDetailStatusCount> {
    return orderedStatuses.mapNotNull { status ->
      val count = items.count { it.item.diffType == status }
      count.takeIf { it > 0 }?.let {
        val statusDisplayData = buildStatusDisplayData(status)
        SnapshotDetailStatusCount(
          count = it,
          countText = NumberFormat.getIntegerInstance().format(it),
          label = context.getString(statusDisplayData.labelRes),
          status = statusDisplayData
        )
      }
    }
  }

  @StringRes
  private fun getSectionTitleRes(@LibType type: Int): Int {
    return when (type) {
      NATIVE -> R.string.ref_category_native
      SERVICE -> R.string.ref_category_service
      ACTIVITY -> R.string.ref_category_activity
      RECEIVER -> R.string.ref_category_br
      PROVIDER -> R.string.ref_category_cp
      PERMISSION -> R.string.ref_category_perm
      METADATA -> R.string.ref_category_metadata
      else -> android.R.string.untitled
    }
  }

  private fun buildStatusDisplayData(status: Int): SnapshotDetailItemStatusDisplayData {
    return when (status) {
      ADDED -> SnapshotDetailItemStatusDisplayData(
        iconRes = R.drawable.ic_add,
        colorRes = R.color.material_green_300,
        countColorRes = R.color.material_green_200,
        labelRes = R.string.snapshot_indicator_added
      )

      REMOVED -> SnapshotDetailItemStatusDisplayData(
        iconRes = R.drawable.ic_remove,
        colorRes = R.color.material_red_300,
        countColorRes = R.color.material_red_200,
        labelRes = R.string.snapshot_indicator_removed
      )

      CHANGED -> SnapshotDetailItemStatusDisplayData(
        iconRes = R.drawable.ic_changed,
        colorRes = R.color.material_yellow_300,
        countColorRes = R.color.material_yellow_200,
        labelRes = R.string.snapshot_indicator_changed
      )

      MOVED -> SnapshotDetailItemStatusDisplayData(
        iconRes = R.drawable.ic_move,
        colorRes = R.color.material_blue_300,
        countColorRes = R.color.material_blue_200,
        labelRes = R.string.snapshot_indicator_moved
      )

      else -> throw IllegalArgumentException("wrong diff type")
    }
  }

  private suspend fun getRule(item: SnapshotDetailItem): Rule? = withContext(Dispatchers.IO) {
    RulesRepository.getRule(item.name, item.itemType, true)
  }

  private fun addComponentDiffInfoFromJson(
    list: MutableList<SnapshotDetailItem>,
    diffNode: SnapshotDiffItem.DiffNode<String>,
    @LibType libType: Int
  ) {
    val old =
      diffNode.old.fromJson<List<String>>(List::class.java, String::class.java).orEmpty().toSet()
    val new =
      diffNode.new?.fromJson<List<String>>(List::class.java, String::class.java)?.toSet()
    list.addAll(getComponentsDiffList(old, new, libType))
  }

  private fun getNativeDiffList(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()
    if (newList == null) {
      return list
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val intersectList = mutableListOf<LibStringItem>()

    for (item in tempNewList) {
      oldList.find { it.name == item.name }?.let {
        if (it.size != item.size) {
          val diffSize = item.size - it.size
          val extra = buildSpannedString {
            append("${it.size.sizeToString(context)} $ARROW ${item.size.sizeToString(context)}")
            appendLine()
            inSpans(ForegroundColorSpan(ColorUtils.setAlphaComponent(Color.BLACK, 165))) {
              if (diffSize > 0) {
                append("+")
              }
              append(diffSize.sizeToString(context))
              append(", ")
              if (diffSize > 0) {
                append("+")
              }
              val percentage = (diffSize.toFloat() / it.size)
              if (abs(percentage) < 0.001f) {
                if (percentage < 0) {
                  append("-")
                }
                append("<0.1%")
              } else {
                append(String.format(Locale.getDefault(), "%.1f%%", percentage * 100))
              }
            }
          }
          list.add(
            SnapshotDetailItem(
              it.name,
              it.name,
              extra,
              CHANGED,
              NATIVE
            )
          )
        }
        intersectList.add(item)
      }
    }

    for (item in intersectList) {
      tempOldList.remove(tempOldList.find { it.name == item.name })
      tempNewList.remove(tempNewList.find { it.name == item.name })
    }

    for (item in tempOldList) {
      list.add(
        SnapshotDetailItem(
          item.name,
          item.name,
          PackageUtils.sizeToString(context, item),
          REMOVED,
          NATIVE
        )
      )
    }
    for (item in tempNewList) {
      list.add(
        SnapshotDetailItem(
          item.name,
          item.name,
          PackageUtils.sizeToString(context, item),
          ADDED,
          NATIVE
        )
      )
    }

    return list
  }

  private fun getComponentsDiffList(
    oldSet: Set<String>,
    newSet: Set<String>?,
    @LibType type: Int
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    if (newSet == null) {
      return emptyList()
    }

    val removeList = (oldSet - newSet).toMutableSet()
    val addList = (newSet - oldSet).toMutableSet()

    val pendingRemovedOldSet = mutableSetOf<String>()
    val pendingRemovedNewSet = mutableSetOf<String>()

    for (item in addList) {
      removeList.find { it.substringAfterLast(".") == item.substringAfterLast(".") }?.let {
        list.add(
          SnapshotDetailItem(item, String.format("%s\n$ARROW\n%s", it, item), "", MOVED, type)
        )
        pendingRemovedOldSet.add(it)
        pendingRemovedNewSet.add(item)
      }
    }
    removeList.removeAll(pendingRemovedOldSet)
    addList.removeAll(pendingRemovedNewSet)

    removeList.forEach {
      list.add(
        SnapshotDetailItem(it, it, "", REMOVED, type)
      )
    }
    addList.forEach {
      list.add(
        SnapshotDetailItem(it, it, "", ADDED, type)
      )
    }

    return list
  }

  private fun getPermissionsDiffList(
    oldSet: Set<String>,
    newSet: Set<String>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    if (newSet == null) {
      return emptyList()
    }

    val removeList = oldSet - newSet
    val addList = newSet - oldSet

    removeList.forEach {
      list.add(
        SnapshotDetailItem(it, it, "", REMOVED, PERMISSION)
      )
    }
    addList.forEach {
      list.add(
        SnapshotDetailItem(it, it, "", ADDED, PERMISSION)
      )
    }

    return list
  }

  private fun getMetadataDiffList(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    if (newList == null) {
      return list
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val intersectList = mutableListOf<LibStringItem>()

    for (item in tempNewList) {
      oldList.find { it.name == item.name }?.let {
        if (it.source != item.source) {
          val extra =
            "${it.source.orEmpty()} $ARROW ${item.source.orEmpty()}"
          list.add(
            SnapshotDetailItem(
              it.name,
              it.name,
              extra,
              CHANGED,
              METADATA
            )
          )
        }
        intersectList.add(item)
      }
    }

    for (item in intersectList) {
      tempOldList.remove(tempOldList.find { it.name == item.name })
      tempNewList.remove(tempNewList.find { it.name == item.name })
    }

    for (item in tempOldList) {
      list.add(
        SnapshotDetailItem(item.name, item.name, item.source.orEmpty(), REMOVED, METADATA)
      )
    }
    for (item in tempNewList) {
      list.add(
        SnapshotDetailItem(item.name, item.name, item.source.orEmpty(), ADDED, METADATA)
      )
    }

    return list
  }

  private companion object {
    const val ARROW = "→"
    val orderedStatuses = listOf(ADDED, REMOVED, CHANGED, MOVED)
    val orderedTypes = listOf(NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, PERMISSION, METADATA)
  }
}
