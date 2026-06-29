package com.absinthe.libchecker.domain.statistics.reference.ui.adapter

import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.LIB_REFERENCE_PROVIDER
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.LibReferenceProvider
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.MULTIPLE_APPS_ICON_PROVIDER
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.MultipleAppsIconProvider
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode

class LibReferenceAdapter(
  initialColorfulRuleIcon: Boolean = true,
  private val onDetailIconClick: (LibReference) -> Unit
) : BaseNodeAdapter(RefListDiffUtil()) {

  var highlightText: String = ""
  var colorfulRuleIcon: Boolean = initialColorfulRuleIcon
    private set

  init {
    addNodeProvider(
      LibReferenceProvider(
        colorfulRuleIcon = { colorfulRuleIcon },
        highlightText = { highlightText },
        onDetailIconClick = onDetailIconClick
      )
    )
    addNodeProvider(MultipleAppsIconProvider(highlightText = { highlightText }))
  }

  fun updateColorfulRuleIcon(enabled: Boolean) {
    if (colorfulRuleIcon == enabled) {
      return
    }
    colorfulRuleIcon = enabled
    // noinspection NotifyDataSetChanged
    notifyDataSetChanged()
  }

  override fun getItemType(data: List<BaseNode>, position: Int): Int {
    val item = (data[position] as? LibReference) ?: return -1
    return when (item.type) {
      PACKAGE, SHARED_UID -> MULTIPLE_APPS_ICON_PROVIDER
      else -> LIB_REFERENCE_PROVIDER
    }
  }
}
