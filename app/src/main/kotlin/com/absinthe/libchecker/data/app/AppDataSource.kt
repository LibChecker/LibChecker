package com.absinthe.libchecker.data.app

import android.content.pm.PackageInfo

interface AppDataSource {

  fun getApplicationList(forceUpdate: Boolean = false): List<PackageInfo>

  fun getApplicationMap(forceUpdate: Boolean = false): Map<String, PackageInfo>
}
