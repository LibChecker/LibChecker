package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.statistics.AppStatisticAnalysisState
import com.absinthe.libchecker.domain.app.detail.statistics.AppStatisticRuleAnalysis
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.ui.loadStatisticIcon
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.progressindicator.CircularProgressIndicator

class AppStatisticAnalysisBottomSheetView(
  context: Context,
  onAnalysisClick: (AppStatisticRuleAnalysis) -> Unit
) : LinearLayout(context),
  IHeaderView {

  private val adapter = AnalysisAdapter(onAnalysisClick)

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.app_detail_online_rules_title)
  }

  private val content = FrameLayout(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, CONTENT_HEIGHT_DP.dp)
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT
    )
    layoutManager = LinearLayoutManager(context)
    adapter = this@AppStatisticAnalysisBottomSheetView.adapter
    itemAnimator = null
    overScrollMode = OVER_SCROLL_NEVER
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    setPadding(0, 4.dp, 0, 12.dp)
  }

  private val progressIndicator = CircularProgressIndicator(context).apply {
    layoutParams = LayoutParams(PROGRESS_SIZE_DP.dp, PROGRESS_SIZE_DP.dp)
    isIndeterminate = false
    max = 100
  }

  private val message = TextView(context).apply {
    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
      it.topMargin = 16.dp
    }
    gravity = Gravity.CENTER
    setTextAppearance(
      context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodyMedium)
    )
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
  }

  private val messageContainer = LinearLayout(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT
    )
    orientation = VERTICAL
    gravity = Gravity.CENTER
    setPadding(24.dp, 24.dp, 24.dp, 24.dp)
    addView(progressIndicator)
    addView(message)
  }

  init {
    orientation = VERTICAL
    addPaddingTop(16.dp)
    content.addView(list)
    content.addView(messageContainer)
    addView(header)
    addView(content)
  }

  fun bind(state: AppStatisticAnalysisState) {
    when (state) {
      AppStatisticAnalysisState.Idle -> showLoading(0)

      is AppStatisticAnalysisState.Loading -> showLoading(state.progress)

      is AppStatisticAnalysisState.Results -> showResults(state.analyses)

      AppStatisticAnalysisState.Empty -> showMessage(
        text = context.getString(R.string.app_detail_online_rules_empty),
        isError = false
      )

      AppStatisticAnalysisState.Error -> showMessage(
        text = context.getString(R.string.app_detail_online_rules_error),
        isError = true
      )
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = header

  private fun showLoading(progress: Int) {
    list.isVisible = false
    messageContainer.isVisible = true
    progressIndicator.isVisible = true
    progressIndicator.setProgressCompat(progress, true)
    message.setTextColor(
      context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
    )
    message.text = context.getString(
      R.string.app_detail_online_rules_loading,
      progress
    )
  }

  private fun showResults(analyses: List<AppStatisticRuleAnalysis>) {
    messageContainer.isVisible = false
    list.isVisible = true
    adapter.setList(analyses)
  }

  private fun showMessage(text: String, isError: Boolean) {
    list.isVisible = false
    messageContainer.isVisible = true
    progressIndicator.isVisible = false
    message.text = text
    message.setTextColor(
      context.getColorByAttr(
        if (isError) {
          androidx.appcompat.R.attr.colorError
        } else {
          com.google.android.material.R.attr.colorOnSurfaceVariant
        }
      )
    )
  }

  private class AnalysisAdapter(
    private val onAnalysisClick: (AppStatisticRuleAnalysis) -> Unit
  ) : BaseQuickAdapter<AppStatisticRuleAnalysis, BaseViewHolder>(0) {

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
      return BaseViewHolder(AppStatisticAnalysisItemView(context))
    }

    override fun convert(holder: BaseViewHolder, item: AppStatisticRuleAnalysis) {
      (holder.itemView as AppStatisticAnalysisItemView).bind(item, onAnalysisClick)
    }
  }

  private companion object {
    const val CONTENT_HEIGHT_DP = 300
    const val PROGRESS_SIZE_DP = 52
  }
}

private class AppStatisticAnalysisItemView(context: Context) : LinearLayout(context) {

  private val icon = ImageView(context).apply {
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    scaleType = ImageView.ScaleType.CENTER_INSIDE
  }

  private val title = TextView(context).apply {
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    setTextAppearance(
      context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleMedium)
    )
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    maxLines = 2
  }

  private val details = TextView(context).apply {
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    setTextAppearance(
      context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodySmall)
    )
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    maxLines = 3
  }

  private val status = TextView(context).apply {
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    setTextAppearance(
      context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelLarge)
    )
    setTypeface(typeface, Typeface.BOLD)
    gravity = Gravity.CENTER
  }

  init {
    layoutParams = RecyclerView.LayoutParams(
      RecyclerView.LayoutParams.MATCH_PARENT,
      RecyclerView.LayoutParams.WRAP_CONTENT
    )
    orientation = HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = ITEM_MIN_HEIGHT_DP.dp
    setPadding(20.dp, 6.dp, 20.dp, 6.dp)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackground))
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

    addView(
      icon,
      LayoutParams(ICON_SIZE_DP.dp, ICON_SIZE_DP.dp).also {
        it.marginEnd = 12.dp
      }
    )
    addView(
      LinearLayout(context).apply {
        orientation = VERTICAL
        addView(title)
        addView(details)
      },
      LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
    )
    addView(
      status,
      LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
        it.marginStart = 12.dp
      }
    )
  }

  fun bind(
    analysis: AppStatisticRuleAnalysis,
    onAnalysisClick: (AppStatisticRuleAnalysis) -> Unit
  ) {
    val titleText = analysis.definition.title.resolve(context)
    val detailText = analysis.resolveDetail(context)
    val statusText = context.getString(
      if (analysis.matched) {
        R.string.app_detail_online_rules_matched
      } else {
        R.string.app_detail_online_rules_unmatched
      }
    )

    icon.loadStatisticIcon(
      icon = analysis.definition.icon,
      selected = analysis.matched,
      grayscale = !analysis.matched
    )
    title.text = titleText
    details.text = detailText
    details.isVisible = detailText.isNotBlank()
    status.text = statusText
    status.setTextColor(
      context.getColorByAttr(
        if (analysis.matched) {
          androidx.appcompat.R.attr.colorPrimary
        } else {
          com.google.android.material.R.attr.colorOnSurfaceVariant
        }
      )
    )
    contentDescription = listOf(titleText, statusText, detailText)
      .filter(String::isNotBlank)
      .joinToString(", ")
    val hasDetails = analysis.definition.details != null
    isClickable = hasDetails
    isFocusable = hasDetails
    setOnClickListener(
      if (hasDetails) {
        OnClickListener { onAnalysisClick(analysis) }
      } else {
        null
      }
    )
  }

  private fun AppStatisticRuleAnalysis.resolveDetail(context: Context): String {
    return when (definition.calculation.kind) {
      StatisticCalculationKind.PREDICATE -> definition.calculation.predicate?.let { predicate ->
        if (matched) predicate.matchedTitle.resolve(context) else predicate.unmatchedTitle.resolve(context)
      }.orEmpty()

      StatisticCalculationKind.FACETS -> definition.calculation.facets?.let { facets ->
        if (!matched) {
          facets.unmatchedTitle.resolve(context)
        } else {
          val matchedIds = matchedFacetIds.toSet()
          facets.items
            .filter { facet -> facet.id in matchedIds }
            .joinToString(" · ") { facet -> facet.shortTitle?.resolve(context) ?: facet.title.resolve(context) }
        }
      }.orEmpty()

      StatisticCalculationKind.NATIVE -> ""
    }
  }

  private companion object {
    const val ITEM_MIN_HEIGHT_DP = 64
    const val ICON_SIZE_DP = 44
  }
}
