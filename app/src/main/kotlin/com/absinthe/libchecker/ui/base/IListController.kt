package com.absinthe.libchecker.ui.base

import androidx.recyclerview.widget.RecyclerView
import rikka.widget.borderview.BorderViewDelegate

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/28
 * </pre>
 */
interface IListController {
  fun onReturnTop()
  fun getBorderViewDelegate(): BorderViewDelegate?
  fun isAllowRefreshing(): Boolean
  fun getSuitableLayoutManager(): RecyclerView.LayoutManager?
}
