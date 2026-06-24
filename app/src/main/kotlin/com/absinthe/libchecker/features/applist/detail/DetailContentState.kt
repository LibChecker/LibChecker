package com.absinthe.libchecker.features.applist.detail

import android.util.SparseArray
import androidx.core.util.forEach
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.constant.AbilityType
import com.absinthe.libchecker.domain.app.detail.AppDetailComponentChips
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.features.statistics.bean.LibStringItemChip
import kotlinx.coroutines.flow.MutableStateFlow

class DetailContentState {
  private var nativeLibItemsByTab: Map<String, List<LibStringItem>> = emptyMap()

  val nativeLibTabs: MutableStateFlow<Collection<String>?> = MutableStateFlow(null)
  val nativeLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val staticLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val metaDataItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val permissionsItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val dexLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val signaturesLibItems: MutableStateFlow<List<LibStringItemChip>?> = MutableStateFlow(null)
  val componentsMap = SparseArray<MutableStateFlow<List<LibStringItemChip>?>>().apply {
    put(SERVICE, MutableStateFlow(null))
    put(ACTIVITY, MutableStateFlow(null))
    put(RECEIVER, MutableStateFlow(null))
    put(PROVIDER, MutableStateFlow(null))
  }
  val abilitiesMap = SparseArray<MutableStateFlow<List<LibStringItemChip>?>>()

  var processesMap: Map<String, Int> = emptyMap()
    private set

  fun reset() {
    nativeLibItemsByTab = emptyMap()
    nativeLibTabs.value = null
    nativeLibItems.value = null
    staticLibItems.value = null
    metaDataItems.value = null
    permissionsItems.value = null
    dexLibItems.value = null
    signaturesLibItems.value = null
    componentsMap.forEach { _, value -> value.value = null }
    abilitiesMap.forEach { _, value -> value.value = null }
    processesMap = emptyMap()
  }

  suspend fun emitNativeLibTabs(itemsByTab: Map<String, List<LibStringItem>>) {
    nativeLibItemsByTab = itemsByTab
    nativeLibTabs.emit(itemsByTab.keys)
    if (itemsByTab.isEmpty()) {
      nativeLibItems.emit(emptyList())
    }
  }

  fun nativeLibItemsFor(tab: String): List<LibStringItem>? {
    return nativeLibItemsByTab[tab]
  }

  fun hasComponentsData(): Boolean {
    return componentsMap[SERVICE]?.value != null &&
      componentsMap[ACTIVITY]?.value != null &&
      componentsMap[RECEIVER]?.value != null &&
      componentsMap[PROVIDER]?.value != null
  }

  suspend fun emitComponents(
    components: AppDetailComponentChips,
    processColor: () -> Int
  ) {
    processesMap = components.processNames.associateWith { processColor() }
    emitComponentItems(components)
  }

  suspend fun emitComponentItems(components: AppDetailComponentChips) {
    componentsMap[SERVICE]?.emit(components.services)
    componentsMap[ACTIVITY]?.emit(components.activities)
    componentsMap[RECEIVER]?.emit(components.receivers)
    componentsMap[PROVIDER]?.emit(components.providers)
  }

  fun resetAbilities() {
    abilitiesMap.put(AbilityType.PAGE, MutableStateFlow(null))
    abilitiesMap.put(AbilityType.SERVICE, MutableStateFlow(null))
    abilitiesMap.put(AbilityType.WEB, MutableStateFlow(null))
    abilitiesMap.put(AbilityType.DATA, MutableStateFlow(null))
  }

  suspend fun emitAbilities(abilityChips: Map<Int, List<LibStringItemChip>>) {
    abilityChips.forEach { (type, items) ->
      abilitiesMap[type]?.emit(items)
    }
  }
}
