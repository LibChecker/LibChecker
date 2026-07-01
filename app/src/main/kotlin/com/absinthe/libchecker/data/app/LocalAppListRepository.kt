package com.absinthe.libchecker.data.app

import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListRepository
import kotlinx.coroutines.flow.Flow

class LocalAppListRepository(
  private val lcRepository: LCRepository
) : AppListRepository {

  override val items: Flow<List<LCItem>> = lcRepository.allLCItemsFlow

  override suspend fun getItems(): List<LCItem> {
    return lcRepository.getLCItems()
  }

  override suspend fun getItem(packageName: String): LCItem? {
    return lcRepository.getItem(packageName)
  }

  override suspend fun getUninitializedFeaturePackageNames(): List<String> {
    return lcRepository.getUninitializedFeaturePackageNames()
  }

  override fun clearItems() {
    lcRepository.deleteAllItems()
  }

  override suspend fun insertItem(item: LCItem) {
    lcRepository.insert(item)
  }

  override suspend fun insertItems(items: List<LCItem>) {
    lcRepository.insert(items)
  }

  override suspend fun updateItem(item: LCItem) {
    lcRepository.update(item)
  }

  override fun updateFeatures(packageName: String, features: Int) {
    lcRepository.updateFeatures(packageName, features)
  }

  override fun updateFeatures(featuresMap: Map<String, Int>) {
    lcRepository.updateFeatures(featuresMap)
  }

  override fun deleteItemByPackageName(packageName: String) {
    lcRepository.deleteLCItemByPackageName(packageName)
  }
}
