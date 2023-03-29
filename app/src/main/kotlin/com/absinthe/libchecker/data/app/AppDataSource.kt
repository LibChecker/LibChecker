package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

interface AppDataSource {
  fun getApplicationList(ioDispatcher: CoroutineDispatcher): Flow<List<PackageInfo>>

  fun getCachedApplicationMap(ioDispatcher: CoroutineDispatcher): Flow<Map<String, PackageInfo>>

  fun getCachedApplicationMap(): Map<String, PackageInfo>

  fun clearCache()
}
