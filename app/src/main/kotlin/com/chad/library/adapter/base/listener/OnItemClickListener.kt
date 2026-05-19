package com.chad.library.adapter.base.listener

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter

interface OnItemClickListener {
  fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int)
}
