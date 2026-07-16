package com.absinthe.libchecker.data.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.app.model.PackageChangeState

interface AppDataSource {

  fun getApplicationList(forceUpdate: Boolean = false): List<PackageInfo>

  fun getApplicationMap(forceUpdate: Boolean = false): Map<String, PackageInfo>

  fun getApplicationCount(forceUpdate: Boolean = false): Int

  fun getRandomApplicationInfo(forceUpdate: Boolean = false): ApplicationInfo?

  fun getApexPackageNames(): Set<String>

  fun updateApplications(state: PackageChangeState)
}
