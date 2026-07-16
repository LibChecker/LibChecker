package com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider

import android.view.View
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceAction
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceItemDisplay
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceListRenderState
import com.absinthe.libchecker.domain.statistics.reference.model.canOpenDetail
import com.absinthe.libchecker.domain.statistics.reference.ui.view.LibReferenceItemView
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.text.NumberFormat

const val LIB_REFERENCE_PROVIDER = 0

class LibReferenceProvider(
  private val renderState: () -> LibReferenceListRenderState,
  private val onAction: (LibReferenceAction) -> Unit
) : BaseNodeProvider() {

  override val itemViewType: Int = LIB_REFERENCE_PROVIDER
  override val layoutId: Int = 0

  init {
    addChildClickViewIds(android.R.id.icon)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      LibReferenceItemView(context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
          it.setMargins(0, margin, 0, margin)
        }
      }
    )
  }

  override fun convert(helper: BaseViewHolder, item: BaseNode) {
    val reference = item as LibReference
    val state = renderState()
    (helper.itemView as LibReferenceItemView).bind(
      display = LibReferenceItemDisplay.create(
        reference = reference,
        colorfulRuleIcon = state.colorfulRuleIcon,
        notMarkedLabel = context.getString(R.string.not_marked_lib),
        countText = NumberFormat.getIntegerInstance().format(reference.referredList.size)
      ),
      highlightText = state.highlightText
    )
  }

  override fun onChildClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
    super.onChildClick(helper, view, data, position)
    if (view.id == android.R.id.icon) {
      val ref = data as? LibReference ?: return
      if (ref.canOpenDetail()) {
        onAction(LibReferenceAction.DetailIconClicked(ref))
      }
    }
  }
}
