package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.database.entity.LCItem

interface AppListItemFactory {

  fun create(packageInfo: PackageInfo, delayInitFeatures: Boolean = false): LCItem
}
