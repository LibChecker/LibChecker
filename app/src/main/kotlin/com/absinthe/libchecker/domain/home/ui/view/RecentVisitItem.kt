package com.absinthe.libchecker.domain.home.ui.view

import android.graphics.drawable.Drawable
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.home.recent.RecentVisit

data class RecentVisitItem(
  val visit: RecentVisit,
  val label: String,
  val icon: Drawable,
  val app: LCItem? = null,
  val tintIcon: Boolean = false
)

data class RecentVisitLists(
  val apps: RecentVisitGroup,
  val libraries: RecentVisitGroup
)

data class RecentVisitGroup(
  val pinned: List<RecentVisitItem> = emptyList(),
  val recent: List<RecentVisitItem> = emptyList()
)
