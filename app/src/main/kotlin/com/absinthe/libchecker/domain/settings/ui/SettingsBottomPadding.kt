package com.absinthe.libchecker.domain.settings.ui

internal fun calculateSettingsBottomPadding(
  basePadding: Int,
  systemBarBottomInset: Int,
  bottomNavigationHeight: Int?
): Int {
  require(basePadding >= 0)
  require(systemBarBottomInset >= 0)
  require(bottomNavigationHeight == null || bottomNavigationHeight >= 0)
  return basePadding + (bottomNavigationHeight ?: systemBarBottomInset)
}
