package com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceListRenderState
import com.absinthe.libchecker.domain.statistics.reference.model.MultipleAppsIconItemDisplay
import com.absinthe.libchecker.domain.statistics.reference.ui.view.MultipleAppsIconItemView
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val MULTIPLE_APPS_ICON_PROVIDER = 1

class MultipleAppsIconProvider(
  private val renderState: () -> LibReferenceListRenderState,
  private val onItemBound: (LibReference, View) -> Unit
) : BaseNodeProvider() {

  private val notMarkedLabel by lazy { context.getString(R.string.not_marked_lib) }
  private val packageLabel by lazy { context.getString(R.string.ref_category_package) }

  override val itemViewType: Int = MULTIPLE_APPS_ICON_PROVIDER
  override val layoutId: Int = 0

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return BaseViewHolder(
      MultipleAppsIconItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
        layoutParams = RecyclerView.LayoutParams(
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
    val state = renderState()
    val reference = item as LibReference
    val sharedUid = reference.iconPackages.firstNotNullOfOrNull { it.applicationInfo?.uid }
    (helper.itemView as MultipleAppsIconItemView).bind(
      display = MultipleAppsIconItemDisplay.create(
        reference = reference,
        notMarkedLabel = notMarkedLabel,
        packageLabel = packageLabel,
        sharedUidLabel = sharedUid?.let { "UID $it" } ?: "UID",
        labelSuffix = state.labelSuffix
      ),
      highlightText = state.highlightText
    )
    onItemBound(reference, helper.itemView)
  }
}
