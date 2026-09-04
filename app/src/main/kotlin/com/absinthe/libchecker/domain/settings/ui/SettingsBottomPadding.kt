package com.absinthe.libchecker.domain.settings.ui

internal fun calculateSettingsBottomPadding(
  basePadding: Int,
  systemBarBottomInset: Int,
  bottomNavigationHeight: Int?,
  bottomNavigationBottomMargin: Int = 0
): Int {
  require(basePadding >= 0)
  require(systemBarBottomInset >= 0)
  require(bottomNavigationHeight == null || bottomNavigationHeight >= 0)
  require(bottomNavigationBottomMargin >= 0)
  return basePadding + (bottomNavigationHeight?.let { it + bottomNavigationBottomMargin } ?: systemBarBottomInset)
}
