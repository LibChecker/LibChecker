package com.chad.library.adapter.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView

interface HeaderFooterSupport {
  val legacyRecyclerViewOrNull: RecyclerView?
  fun legacyItemCount(): Int
  fun hasFooterLayout(): Boolean
  fun setFooterView(view: View)
  fun removeAllFooterView()
}
