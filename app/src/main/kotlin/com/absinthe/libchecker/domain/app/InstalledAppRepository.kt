package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import kotlinx.coroutines.flow.SharedFlow

interface InstalledAppRepository {

  val packageChanges: SharedFlow<PackageChangeState>

  fun getApplicationList(forceUpdate: Boolean = false): List<PackageInfo>

  fun getApplicationMap(forceUpdate: Boolean = false): Map<String, PackageInfo>

  fun getApplicationCount(forceUpdate: Boolean = false): Int

  fun getPackageInfo(packageName: String, flags: Int = 0): PackageInfo?

  fun isPackageInstalled(packageName: String): Boolean
}
