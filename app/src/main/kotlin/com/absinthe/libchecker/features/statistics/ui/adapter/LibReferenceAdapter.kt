package com.absinthe.libchecker.features.statistics.ui.adapter

import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.features.statistics.bean.LibReference
import com.absinthe.libchecker.features.statistics.ui.adapter.provider.LIB_REFERENCE_PROVIDER
import com.absinthe.libchecker.features.statistics.ui.adapter.provider.LibReferenceProvider
import com.absinthe.libchecker.features.statistics.ui.adapter.provider.MULTIPLE_APPS_ICON_PROVIDER
import com.absinthe.libchecker.features.statistics.ui.adapter.provider.MultipleAppsIconProvider
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode

class LibReferenceAdapter : BaseNodeAdapter() {

  init {
    addNodeProvider(LibReferenceProvider())
    addNodeProvider(MultipleAppsIconProvider())
  }

  override fun getItemType(data: List<BaseNode>, position: Int): Int {
    val item = (data[position] as? LibReference) ?: return -1
    return when (item.type) {
      PACKAGE, SHARED_UID -> MULTIPLE_APPS_ICON_PROVIDER
      else -> LIB_REFERENCE_PROVIDER
    }
  }

  companion object {
    var highlightText: String = String()
  }
}
