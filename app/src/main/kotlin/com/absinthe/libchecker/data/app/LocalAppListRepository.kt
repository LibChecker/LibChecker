package com.absinthe.libchecker.data.app

import com.absinthe.libchecker.database.LCDao
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import kotlinx.coroutines.flow.Flow

class LocalAppListRepository(
  private val dao: LCDao
) : AppListRepository {

  override val items: Flow<List<LCItem>> = dao.getItemsFlow()

  override suspend fun getItems(): List<LCItem> {
    return dao.getItems()
  }

  override suspend fun getItem(packageName: String): LCItem? {
    return dao.getItem(packageName)
  }

  override suspend fun getUninitializedFeaturePackageNames(): List<String> {
    return dao.getUninitializedFeaturePackageNames()
  }

  override suspend fun clearItems() {
    dao.deleteAllItems()
  }

  override suspend fun insertItem(item: LCItem) {
    dao.insert(item)
  }

  override suspend fun insertItems(items: List<LCItem>) {
    dao.insert(items)
  }

  override suspend fun updateItem(item: LCItem) {
    dao.update(item)
  }

  override suspend fun updateFeatures(packageName: String, features: Int) {
    dao.updateFeatures(packageName, features)
  }

  override suspend fun updateFeatures(featuresMap: Map<String, Int>) {
    dao.updateFeatures(featuresMap)
  }

  override suspend fun deleteItemByPackageName(packageName: String) {
    dao.deleteLCItemByPackageName(packageName)
  }
}
