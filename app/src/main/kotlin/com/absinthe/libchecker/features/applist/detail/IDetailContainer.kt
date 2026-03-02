package com.absinthe.libchecker.features.applist.detail

import com.absinthe.libchecker.features.applist.DetailFragmentManager

/**
 * <pre>
 * author : Absinthe
 * time : 2020/09/17
 * </pre>
 */
interface IDetailContainer {
  var detailFragmentManager: DetailFragmentManager
  fun collapseAppBar()
}
