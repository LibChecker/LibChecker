package com.absinthe.libchecker.domain.snapshot

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.database.entity.SnapshotItem

interface SnapshotItemFactory {
  fun create(packageManager: PackageManager, packageInfo: PackageInfo): SnapshotItem
}
