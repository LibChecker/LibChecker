package com.absinthe.libchecker.domain.snapshot.detail.usecase

import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.domain.snapshot.GetSnapshotRuleUseCase
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDetailItem
import com.absinthe.rulesbundle.Rule

class BuildSnapshotDetailSectionsUseCase(
  private val appListSettingsRepository: AppListSettingsRepository,
  private val getSnapshotRule: GetSnapshotRuleUseCase
) {

  suspend operator fun invoke(items: List<SnapshotDetailItem>): List<SnapshotDetailSection> {
    val colorfulRuleIcon = appListSettingsRepository.colorfulRuleIcon
    val ruleCache = mutableMapOf<String, Rule?>()

    suspend fun getRuleCached(item: SnapshotDetailItem): Rule? {
      val key = "${item.itemType}:${item.name}"
      if (ruleCache.containsKey(key)) {
        return ruleCache[key]
      }
      return getSnapshotRule(item).also {
        ruleCache[key] = it
      }
    }

    return orderedTypes.mapNotNull { type ->
      val sectionItems = items
        .filter { it.itemType == type }
        .map { item ->
          SnapshotDetailItemDisplayData(
            item = item,
            rule = getRuleCached(item),
            colorfulRuleIcon = colorfulRuleIcon
          )
        }
      if (sectionItems.isEmpty()) {
        null
      } else {
        SnapshotDetailSection(type, sectionItems)
      }
    }
  }

  private companion object {
    val orderedTypes = listOf(NATIVE, SERVICE, ACTIVITY, RECEIVER, PROVIDER, PERMISSION, METADATA)
  }
}
