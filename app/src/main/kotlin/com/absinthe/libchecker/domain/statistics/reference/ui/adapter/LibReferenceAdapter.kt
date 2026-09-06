package com.absinthe.libchecker.domain.statistics.reference.ui.adapter

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceAction
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceItemDisplay
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceListRenderState
import com.absinthe.libchecker.domain.statistics.reference.model.MultipleAppsIconItemDisplay
import com.absinthe.libchecker.domain.statistics.reference.model.canOpenDetail
import com.absinthe.libchecker.domain.statistics.reference.ui.view.LibReferenceItemView
import com.absinthe.libchecker.domain.statistics.reference.ui.view.MultipleAppsIconItemView
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.text.NumberFormat

const val LIB_REFERENCE_PROVIDER = 0
const val MULTIPLE_APPS_ICON_PROVIDER = 1

class LibReferenceAdapter(
  private val allowDetailAction: Boolean = true,
  private val onItemBound: (LibReference, View) -> Unit = { _, _ -> },
  private val onAction: (LibReferenceAction) -> Unit
) : BaseQuickAdapter<LibReference, BaseViewHolder>(diffCallback = RefListDiffUtil()) {

  private var renderState = LibReferenceListRenderState()
  private val integerFormat by lazy { NumberFormat.getIntegerInstance() }
  private val metadataLabel by lazy { context.getString(R.string.ref_category_metadata) }
  private val notMarkedLabel by lazy { context.getString(R.string.not_marked_lib) }
  private val permissionFallbackLabel by lazy { context.getString(R.string.ref_category_perm) }
  private val packageLabel by lazy { context.getString(R.string.ref_category_package) }

  fun bind(state: LibReferenceListRenderState) {
    renderState = state
  }

  override fun getItemViewType(position: Int, list: List<LibReference>): Int {
    return when (list[position].type) {
      PACKAGE, SHARED_UID -> MULTIPLE_APPS_ICON_PROVIDER
      else -> LIB_REFERENCE_PROVIDER
    }
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    val view = when (viewType) {
      LIB_REFERENCE_PROVIDER -> LibReferenceItemView(context)
      MULTIPLE_APPS_ICON_PROVIDER -> MultipleAppsIconItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard))
      else -> error("Unknown reference view type: $viewType")
    }
    view.layoutParams = RecyclerView.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
      it.setMargins(0, margin, 0, margin)
    }
    return BaseViewHolder(view)
  }

  override fun convert(holder: BaseViewHolder, item: LibReference) {
    val state = renderState
    when (val view = holder.itemView) {
      is LibReferenceItemView -> {
        view.bind(
          display = LibReferenceItemDisplay.create(
            reference = item,
            colorfulRuleIcon = state.colorfulRuleIcon,
            notMarkedLabel = notMarkedLabel,
            permissionFallbackLabel = permissionFallbackLabel,
            metadataLabel = metadataLabel,
            countText = integerFormat.format(item.referredList.size),
            allowDetailAction = allowDetailAction,
            labelSuffix = state.labelSuffix
          ),
          highlightText = state.highlightText
        )
        onItemBound(item, view)
        if (allowDetailAction) {
          view.findViewById<View>(android.R.id.icon).setOnClickListener {
            if (item.canOpenDetail(allowDetailAction)) {
              onAction(LibReferenceAction.DetailIconClicked(item))
            }
          }
        }
      }

      is MultipleAppsIconItemView -> {
        val sharedUid = item.iconPackages.firstNotNullOfOrNull { it.applicationInfo?.uid }
        view.bind(
          display = MultipleAppsIconItemDisplay.create(
            reference = item,
            notMarkedLabel = notMarkedLabel,
            packageLabel = packageLabel,
            sharedUidLabel = sharedUid?.let { "UID $it" } ?: "UID",
            labelSuffix = state.labelSuffix
          ),
          highlightText = state.highlightText
        )
        onItemBound(item, view)
      }
    }
  }
}
