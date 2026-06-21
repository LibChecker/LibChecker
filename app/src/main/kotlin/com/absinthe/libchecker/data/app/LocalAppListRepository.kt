package com.absinthe.libchecker.data.app

import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListRepository
import kotlinx.coroutines.flow.Flow

object LocalAppListRepository : AppListRepository {

  override val items: Flow<List<LCItem>> = Repositories.lcRepository.allLCItemsFlow

  override suspend fun getItems(): List<LCItem> {
    return Repositories.lcRepository.getLCItems()
  }

  override suspend fun getItem(packageName: String): LCItem? {
    return Repositories.lcRepository.getItem(packageName)
  }

  override fun clearItems() {
    Repositories.lcRepository.deleteAllItems()
  }

  override suspend fun insertItem(item: LCItem) {
    Repositories.lcRepository.insert(item)
  }

  override suspend fun insertItems(items: List<LCItem>) {
    Repositories.lcRepository.insert(items)
  }

  override suspend fun updateItem(item: LCItem) {
    Repositories.lcRepository.update(item)
  }

  override fun deleteItemByPackageName(packageName: String) {
    Repositories.lcRepository.deleteLCItemByPackageName(packageName)
  }
}
