package com.absinthe.libchecker.ui.fragment

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import rikka.widget.borderview.BorderViewDelegate

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/28
 * </pre>
 */
interface IListController {
  fun onReturnTop()
  fun getAppBar(): AppBarLayout?
  fun getBorderViewDelegate(): BorderViewDelegate?
  fun scheduleAppbarRaisingStatus()
  fun isAllowRefreshing(): Boolean
  fun getSuitableLayoutManager(): RecyclerView.LayoutManager?
}
