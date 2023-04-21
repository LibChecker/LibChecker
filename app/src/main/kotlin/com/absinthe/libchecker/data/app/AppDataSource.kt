package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

interface AppDataSource {
  fun getApplicationList(ioDispatcher: CoroutineDispatcher): Flow<List<PackageInfo>>

  fun getApplicationList(): List<PackageInfo>

  fun getApplicationMap(ioDispatcher: CoroutineDispatcher): Flow<Map<String, PackageInfo>>

  fun getApplicationMap(): Map<String, PackageInfo>
}
