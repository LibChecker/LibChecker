package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import kotlinx.coroutines.flow.SharedFlow

interface InstalledAppRepository {

  val packageChanges: SharedFlow<PackageChangeState>

  fun getApplicationList(forceUpdate: Boolean = false): List<PackageInfo>

  fun getApplicationMap(forceUpdate: Boolean = false): Map<String, PackageInfo>

  fun getPackageInfo(packageName: String): PackageInfo?
}
