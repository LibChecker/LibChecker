package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.database.entity.LCItem
import kotlinx.coroutines.flow.Flow

interface AppListRepository {

  val items: Flow<List<LCItem>>

  suspend fun getItems(): List<LCItem>

  suspend fun getItem(packageName: String): LCItem?

  fun clearItems()

  suspend fun insertItem(item: LCItem)

  suspend fun insertItems(items: List<LCItem>)

  suspend fun updateItem(item: LCItem)

  fun deleteItemByPackageName(packageName: String)
}
