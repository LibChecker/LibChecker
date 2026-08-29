package com.absinthe.libchecker.domain.statistics.reference.ui.adapter

import android.view.View
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceAction
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceListRenderState
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.LIB_REFERENCE_PROVIDER
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.LibReferenceProvider
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.MULTIPLE_APPS_ICON_PROVIDER
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.MultipleAppsIconProvider
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode

class LibReferenceAdapter(
  private val allowDetailAction: Boolean = true,
  private val onItemBound: (LibReference, View) -> Unit = { _, _ -> },
  private val onAction: (LibReferenceAction) -> Unit
) : BaseNodeAdapter(RefListDiffUtil()) {

  private var renderState = LibReferenceListRenderState()

  init {
    addNodeProvider(
      LibReferenceProvider(
        renderState = { renderState },
        allowDetailAction = allowDetailAction,
        onAction = onAction,
        onItemBound = onItemBound
      )
    )
    addNodeProvider(
      MultipleAppsIconProvider(
        renderState = { renderState },
        onItemBound = onItemBound
      )
    )
  }

  fun bind(state: LibReferenceListRenderState) {
    renderState = state
  }

  override fun getItemType(data: List<BaseNode>, position: Int): Int {
    val item = (data[position] as? LibReference) ?: return -1
    return when (item.type) {
      PACKAGE, SHARED_UID -> MULTIPLE_APPS_ICON_PROVIDER
      else -> LIB_REFERENCE_PROVIDER
    }
  }
}
