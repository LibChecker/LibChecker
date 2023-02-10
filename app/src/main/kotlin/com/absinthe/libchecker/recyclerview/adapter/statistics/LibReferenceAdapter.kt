package com.absinthe.libchecker.recyclerview.adapter.statistics

import android.view.ViewGroup
import android.widget.Space
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.bean.LibReference
import com.absinthe.libchecker.recyclerview.adapter.statistics.provider.LIB_REFERENCE_PROVIDER
import com.absinthe.libchecker.recyclerview.adapter.statistics.provider.LibReferenceProvider
import com.absinthe.libchecker.recyclerview.adapter.statistics.provider.MULTIPLE_APPS_ICON_PROVIDER
import com.absinthe.libchecker.recyclerview.adapter.statistics.provider.MultipleAppsIconProvider
import com.absinthe.libchecker.utils.extensions.dp
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode

class LibReferenceAdapter : BaseNodeAdapter() {

  private var footer: Space? = null

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

  fun setSpaceFooterView() {
    recyclerViewOrNull?.let { rv ->
      if (!rv.canScrollVertically(1)) {
        if (footer != null) return
        Space(rv.context).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            96.dp
          )
        }.also {
          setFooterView(it)
          footer = it
        }
      } else {
        if (footer == null) return
        removeFooterView(footer!!)
        footer = null
      }
    }
  }

  companion object {
    var highlightText: String = ""
  }
}
