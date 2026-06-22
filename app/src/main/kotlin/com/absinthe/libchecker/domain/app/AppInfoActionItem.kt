package com.absinthe.libchecker.domain.app

import android.content.Intent
import android.content.pm.PackageItemInfo

data class AppInfoActionItem(
  val packageItemInfo: PackageItemInfo,
  val intent: Intent
)
