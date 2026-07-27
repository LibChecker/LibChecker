package com.absinthe.libchecker.domain.snapshot.detail.usecase

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.text.buildSpannedString
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.repository.AppListSettingsRepository
import com.absinthe.libchecker.domain.snapshot.detail.model.SNAPSHOT_DETAIL_DIFF_ARROW
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailContent
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailDiffTextStyle
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemStatusDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailStatusCount
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailItemDescription
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportItemText
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportSectionText
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailRuleChipDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailSectionDescription
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailSummary
import com.absinthe.libchecker.domain.snapshot.detail.model.colorSnapshotDetailMetricDeltas
import com.absinthe.libchecker.domain.snapshot.detail.model.emphasizeSnapshotDetailDiffArrows
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.dex.DexEntryInfo
import com.absinthe.libchecker.utils.dex.ResourceEntryInfo
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.fromJson
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

  suspend operator fun invoke(
    item: SnapshotDiffItem,
    diffTextStyle: SnapshotDetailDiffTextStyle
  ): SnapshotDetailContent = withContext(Dispatchers.IO) {
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

    list.addAll(
      getDexDiffList(
        item.dexInfoDiff.old.fromJson<List<DexEntryInfo>>(
          List::class.java,
          DexEntryInfo::class.java
        ) ?: emptyList(),
        item.dexInfoDiff.new?.fromJson<List<DexEntryInfo>>(
          List::class.java,
          DexEntryInfo::class.java
        )
      )
    )
    val resourceDiffItems = getResourceDiffList(
      item.resourceInfoDiff.old.fromJson<List<ResourceEntryInfo>>(
        List::class.java,
        ResourceEntryInfo::class.java
      ) ?: emptyList(),
      item.resourceInfoDiff.new?.fromJson<List<ResourceEntryInfo>>(
        List::class.java,
        ResourceEntryInfo::class.java
      )
    )
    if (resourceDiffItems.isEmpty()) {
      getResourcesDiffItem(item.resourcesSizeDiff)?.let(list::add)
    } else {
      list.addAll(resourceDiffItems)
    }

    val sections = buildSections(list, diffTextStyle)
    SnapshotDetailContent(
      sections = sections,
      summary = buildSnapshotDetailSummary(sections) { count ->
        context.resources.getQuantityString(
          R.plurals.snapshot_detail_changes_count,
          count,
          count
        )
      }
    )
  }

  private suspend fun buildSections(
    items: List<SnapshotDetailItem>,
    diffTextStyle: SnapshotDetailDiffTextStyle
  ): List<SnapshotDetailSection> {
    val colorfulRuleIcon = appListSettingsRepository.colorfulRuleIcon
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
          val extra = buildExtraDisplayText(item, diffTextStyle)
          SnapshotDetailItemDisplayData(
            item = item,
            title = item.title,
            extra = extra,
            description = buildSnapshotDetailItemDescription(
              statusLabel = context.getString(status.labelRes),
              title = item.title,
              extra = extra,
              ruleLabel = ruleChip?.label
            ),
            reportText = buildSnapshotDetailReportItemText(item),
            status = status,
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
          reportText = buildSnapshotDetailReportSectionText(title),
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

  private fun buildExtraDisplayText(
    item: SnapshotDetailItem,
    style: SnapshotDetailDiffTextStyle
  ): CharSequence {
    val highlighted = if (item.diffType == CHANGED && (style.highlightColor != null || style.emphasizeDiffs)) {
      highlightChangedExtra(item.extra, style.highlightColor, style.emphasizeDiffs)
    } else {
      item.extra
    }
    val arrowEmphasized = highlighted.emphasizeSnapshotDetailDiffArrows(style.arrowColor)
    return if (item.itemType == NATIVE || item.itemType == DEX) {
      arrowEmphasized.colorSnapshotDetailMetricDeltas(style.metricDeltaColor)
    } else {
      arrowEmphasized
    }
  }

  private fun highlightChangedExtra(
    extra: CharSequence,
    @ColorInt highlightColor: Int?,
    emphasizeDiffs: Boolean
  ): CharSequence {
    return highlightSnapshotDetailChangedLines(extra, highlightColor, emphasizeDiffs)
  }

  private fun buildStatusCounts(items: List<SnapshotDetailItemDisplayData>): List<SnapshotDetailStatusCount> {
    return orderedStatuses.mapNotNull { status ->
      val count = items.count { it.item.diffType == status }
      count.takeIf { it > 0 }?.let {
        val statusDisplayData = buildStatusDisplayData(status)
        SnapshotDetailStatusCount(
          diffType = status,
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
      DEX -> R.string.ref_category_package
      else -> android.R.string.untitled
    }
  }

  private fun buildStatusDisplayData(status: Int): SnapshotDetailItemStatusDisplayData {
    return when (status) {
      ADDED -> SnapshotDetailItemStatusDisplayData(
        iconRes = R.drawable.ic_add,
        colorRes = R.color.snapshot_status_added,
        labelRes = R.string.snapshot_indicator_added
      )

      REMOVED -> SnapshotDetailItemStatusDisplayData(
        iconRes = R.drawable.ic_remove,
        colorRes = R.color.snapshot_status_removed,
        labelRes = R.string.snapshot_indicator_removed
      )

      CHANGED -> SnapshotDetailItemStatusDisplayData(
        iconRes = R.drawable.ic_changed,
        colorRes = R.color.snapshot_status_changed,
        labelRes = R.string.snapshot_indicator_changed
      )

      MOVED -> SnapshotDetailItemStatusDisplayData(
        iconRes = R.drawable.ic_move,
        colorRes = R.color.snapshot_status_moved,
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
          val extra = buildString {
            append("${it.size.sizeToString(context)} $ARROW ${item.size.sizeToString(context)}")
            appendLine()
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
          SnapshotDetailItem(
            name = item,
            title = String.format("%s\n$ARROW\n%s", it, item),
            extra = "",
            diffType = MOVED,
            itemType = type,
            previousName = it
          )
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

  private fun getDexDiffList(
    oldList: List<DexEntryInfo>,
    newList: List<DexEntryInfo>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()
    if (newList == null) return list

    val oldByName = oldList.associateBy { it.name }
    val newByName = newList.associateBy { it.name }
    val allEntryNames = (oldByName.keys + newByName.keys).toList()

    for ((name, newEntry) in newByName) {
      val oldEntry = oldByName[name]
      val displayName = buildDexDisplayName(name, allEntryNames)
      if (oldEntry == null) {
        list.add(
          SnapshotDetailItem(
            name = name,
            title = displayName,
            extra = buildDexExtra(newEntry),
            diffType = ADDED,
            itemType = DEX
          )
        )
      } else if (oldEntry != newEntry) {
        list.add(
          SnapshotDetailItem(
            name = name,
            title = displayName,
            extra = buildDexChangedExtra(
              oldEntry = oldEntry,
              newEntry = newEntry,
              contentChangedText = context.getString(R.string.snapshot_content_changed),
              formatSize = { it.sizeToString(context) },
              formatClassCount = { count ->
                context.resources.getQuantityString(
                  R.plurals.snapshot_dex_classes_count,
                  count,
                  NumberFormat.getIntegerInstance().format(count)
                )
              },
              formatSizeDelta = { it.sizeToString(context) }
            ),
            diffType = CHANGED,
            itemType = DEX
          )
        )
      }
    }

    for ((name, oldEntry) in oldByName) {
      if (name !in newByName) {
        list.add(
          SnapshotDetailItem(
            name = name,
            title = buildDexDisplayName(name, allEntryNames),
            extra = buildDexExtra(oldEntry),
            diffType = REMOVED,
            itemType = DEX
          )
        )
      }
    }

    return list
  }

  private fun buildDexExtra(entry: DexEntryInfo): String {
    return buildString {
      append(entry.size.sizeToString(context))
      appendLine()
      append(
        context.resources.getQuantityString(
          R.plurals.snapshot_dex_classes_count,
          entry.classCount,
          NumberFormat.getIntegerInstance().format(entry.classCount)
        )
      )
    }
  }

  private fun getResourceDiffList(
    oldList: List<ResourceEntryInfo>,
    newList: List<ResourceEntryInfo>?
  ): List<SnapshotDetailItem> {
    if (newList == null) return emptyList()

    val list = mutableListOf<SnapshotDetailItem>()
    val oldByName = oldList.associateBy(ResourceEntryInfo::name)
    val newByName = newList.associateBy(ResourceEntryInfo::name)
    val allEntryNames = (oldByName.keys + newByName.keys).toList()

    for ((name, newEntry) in newByName) {
      val oldEntry = oldByName[name]
      val displayName = buildDexDisplayName(name, allEntryNames)
      when {
        oldEntry == null -> list.add(
          SnapshotDetailItem(
            name = name,
            title = displayName,
            extra = newEntry.size.sizeToString(context),
            diffType = ADDED,
            itemType = DEX
          )
        )

        oldEntry != newEntry -> list.add(
          SnapshotDetailItem(
            name = name,
            title = displayName,
            extra = buildResourceChangedExtra(
              oldEntry = oldEntry,
              newEntry = newEntry,
              contentChangedText = context.getString(R.string.snapshot_content_changed),
              formatSize = { it.sizeToString(context) },
              formatSizeDelta = { it.sizeToString(context) }
            ),
            diffType = CHANGED,
            itemType = DEX
          )
        )
      }
    }

    for ((name, oldEntry) in oldByName) {
      if (name !in newByName) {
        list.add(
          SnapshotDetailItem(
            name = name,
            title = buildDexDisplayName(name, allEntryNames),
            extra = oldEntry.size.sizeToString(context),
            diffType = REMOVED,
            itemType = DEX
          )
        )
      }
    }
    return list
  }

  private fun getResourcesDiffItem(
    diffNode: SnapshotDiffItem.DiffNode<Long>
  ): SnapshotDetailItem? {
    val newSize = diffNode.new ?: return null
    if (diffNode.old == newSize) return null
    return SnapshotDetailItem(
      name = RESOURCES_ARSC,
      title = RESOURCES_ARSC,
      extra = buildSizeChangedExtra(
        oldSize = diffNode.old,
        newSize = newSize,
        formatSize = { it.sizeToString(context) },
        formatSizeDelta = { it.sizeToString(context) }
      ),
      diffType = CHANGED,
      itemType = DEX
    )
  }

  private companion object {
    const val ARROW = "→"
    const val RESOURCES_ARSC = "resources.arsc"
    val orderedStatuses = listOf(ADDED, REMOVED, CHANGED, MOVED)
    val orderedTypes = listOf(NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, PERMISSION, METADATA, DEX)
  }
}

internal fun highlightSnapshotDetailChangedLines(
  extra: CharSequence,
  @ColorInt highlightColor: Int?,
  emphasizeDiffs: Boolean
): CharSequence {
  val separator = " $SNAPSHOT_DETAIL_DIFF_ARROW "
  return buildSpannedString {
    parseSnapshotDetailChangedLines(extra).forEachIndexed { index, line ->
      if (index > 0) {
        append('\n')
      }
      if (line.oldValue == null || line.newValue == null) {
        append(line.rawText)
        return@forEachIndexed
      }

      val highlighted = LCAppUtils.getHighlightDifferences(
        oldString = line.oldValue,
        newString = line.newValue,
        highlightDiffColor = highlightColor,
        emphasizeDiffs = emphasizeDiffs
      )
      append(highlighted.first)
      append(separator)
      append(highlighted.second)
    }
  }
}

internal fun parseSnapshotDetailChangedLines(
  extra: CharSequence
): List<SnapshotDetailChangedLine> {
  val separator = " $SNAPSHOT_DETAIL_DIFF_ARROW "
  return extra.toString().split('\n').map { line ->
    val separatorStart = line.indexOf(separator)
    val newValueStart = separatorStart + separator.length
    if (separatorStart <= 0 || newValueStart >= line.length) {
      SnapshotDetailChangedLine(line, null, null)
    } else {
      SnapshotDetailChangedLine(
        rawText = line,
        oldValue = line.substring(0, separatorStart),
        newValue = line.substring(newValueStart)
      )
    }
  }
}

internal data class SnapshotDetailChangedLine(
  val rawText: String,
  val oldValue: String?,
  val newValue: String?
)

internal fun buildDexChangedExtra(
  oldEntry: DexEntryInfo,
  newEntry: DexEntryInfo,
  contentChangedText: String,
  formatSize: (Long) -> String,
  formatClassCount: (Int) -> String,
  formatSizeDelta: (Long) -> String
): String {
  if (
    oldEntry.crc32 != newEntry.crc32 &&
    oldEntry.size == newEntry.size &&
    oldEntry.classCount == newEntry.classCount
  ) {
    return "$contentChangedText\n${formatSize(newEntry.size)}\n" +
      formatClassCount(newEntry.classCount)
  }
  val sizeExtra = buildMetricChangedExtra(
    oldValue = oldEntry.size,
    newValue = newEntry.size,
    formatValue = formatSize,
    formatDelta = formatSizeDelta
  )
  val classesExtra = buildMetricChangedExtra(
    oldValue = oldEntry.classCount.toLong(),
    newValue = newEntry.classCount.toLong(),
    formatValue = { formatClassCount(it.toInt()) },
    formatDelta = { formatClassCount(it.toInt()) }
  )
  return "$sizeExtra\n$classesExtra"
}

internal fun buildResourceChangedExtra(
  oldEntry: ResourceEntryInfo,
  newEntry: ResourceEntryInfo,
  contentChangedText: String,
  formatSize: (Long) -> String,
  formatSizeDelta: (Long) -> String
): String {
  return if (oldEntry.size == newEntry.size && oldEntry.crc32 != newEntry.crc32) {
    "$contentChangedText\n${formatSize(newEntry.size)}"
  } else {
    buildSizeChangedExtra(
      oldSize = oldEntry.size,
      newSize = newEntry.size,
      formatSize = formatSize,
      formatSizeDelta = formatSizeDelta
    )
  }
}

internal fun buildSizeChangedExtra(
  oldSize: Long,
  newSize: Long,
  formatSize: (Long) -> String,
  formatSizeDelta: (Long) -> String
): String {
  return buildMetricChangedExtra(
    oldValue = oldSize,
    newValue = newSize,
    formatValue = formatSize,
    formatDelta = formatSizeDelta
  )
}

internal fun buildDexDisplayName(
  name: String,
  allEntryNames: Collection<String>
): String {
  val hasSplitSource = allEntryNames.any { it.startsWith("split:") }
  return if (!hasSplitSource) name.removePrefix("base/") else name
}

private fun buildMetricChangedExtra(
  oldValue: Long,
  newValue: Long,
  formatValue: (Long) -> String,
  formatDelta: (Long) -> String
): String {
  val delta = newValue - oldValue
  return buildString {
    append(formatValue(oldValue))
    append(" $SNAPSHOT_DETAIL_DIFF_ARROW ")
    append(formatValue(newValue))
    if (delta != 0L) {
      appendLine()
      append(if (delta > 0L) "+" else "-")
      append(formatDelta(abs(delta)))
      if (oldValue != 0L) {
        append(", ")
        if (delta > 0L) {
          append("+")
        }
        append(String.format(Locale.getDefault(), "%.1f%%", delta.toDouble() / oldValue * 100))
      }
    }
  }
}
