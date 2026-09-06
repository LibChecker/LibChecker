package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED

fun buildSnapshotDetailSignedCountText(diffType: Int, countText: String): String {
  return when (diffType) {
    ADDED -> "+$countText"
    REMOVED -> "−$countText"
    CHANGED -> "~$countText"
    MOVED -> "↔$countText"
    else -> countText
  }
}
