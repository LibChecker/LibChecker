package com.absinthe.libchecker.domain.app.repository

import com.absinthe.libchecker.database.entity.LCItem
import kotlinx.coroutines.flow.Flow

interface AppListRepository {

  val items: Flow<List<LCItem>>

  suspend fun getItems(): List<LCItem>

  suspend fun getItem(packageName: String): LCItem?

  suspend fun getUninitializedFeaturePackageNames(): List<String>

  suspend fun clearItems()

  suspend fun insertItem(item: LCItem)

  suspend fun insertItems(items: List<LCItem>)

  suspend fun updateItem(item: LCItem)

  suspend fun updateFeatures(packageName: String, features: Int)

  suspend fun updateFeatures(featuresMap: Map<String, Int>)

  suspend fun deleteItemByPackageName(packageName: String)
}
