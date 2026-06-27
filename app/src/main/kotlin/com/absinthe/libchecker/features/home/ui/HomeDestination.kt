package com.absinthe.libchecker.features.home.ui

import android.content.Intent
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.statistics.reference.ui.LibReferenceFragment
import com.absinthe.libchecker.features.applist.ui.AppListFragment
import com.absinthe.libchecker.features.settings.ui.SettingsFragment
import com.absinthe.libchecker.features.snapshot.ui.SnapshotFragment

enum class HomeDestination(
  val pageIndex: Int,
  @IdRes val navigationItemId: Int,
  val launchAction: String?
) {
  APP_LIST(
    pageIndex = 0,
    navigationItemId = R.id.navigation_app_list,
    launchAction = Constants.ACTION_APP_LIST
  ) {
    override fun createFragment(): Fragment = AppListFragment()
  },

  STATISTICS(
    pageIndex = 1,
    navigationItemId = R.id.navigation_classify,
    launchAction = Constants.ACTION_STATISTICS
  ) {
    override fun createFragment(): Fragment = LibReferenceFragment()
  },

  SNAPSHOT(
    pageIndex = 2,
    navigationItemId = R.id.navigation_snapshot,
    launchAction = Constants.ACTION_SNAPSHOT
  ) {
    override fun createFragment(): Fragment = SnapshotFragment()
  },

  SETTINGS(
    pageIndex = 3,
    navigationItemId = R.id.navigation_settings,
    launchAction = Intent.ACTION_APPLICATION_PREFERENCES
  ) {
    override fun createFragment(): Fragment = SettingsFragment()
  };

  abstract fun createFragment(): Fragment

  companion object {
    val pageCount: Int
      get() = entries.size

    fun requirePageIndex(pageIndex: Int): HomeDestination {
      return fromPageIndex(pageIndex) ?: error("Unknown home destination page index: $pageIndex")
    }

    fun fromPageIndex(pageIndex: Int): HomeDestination? {
      return entries.firstOrNull { it.pageIndex == pageIndex }
    }

    fun fromNavigationItemId(@IdRes itemId: Int): HomeDestination? {
      return entries.firstOrNull { it.navigationItemId == itemId }
    }

    fun fromLaunchAction(action: String?): HomeDestination? {
      return entries.firstOrNull { it.launchAction == action }
    }
  }
}
