package com.absinthe.libchecker.domain.app

import android.content.pm.PackageInfo
import com.absinthe.libchecker.database.entity.LCItem

data class RelatedAppListItem(
  val item: LCItem,
  val packageInfo: PackageInfo?
)
