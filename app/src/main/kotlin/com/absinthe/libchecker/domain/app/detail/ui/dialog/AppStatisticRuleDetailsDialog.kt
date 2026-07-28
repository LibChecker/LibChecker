package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.statistics.AppStatisticRuleAnalysis
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.domain.statistics.chart.ui.resolveDrawable
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object AppStatisticRuleDetailsDialog {

  fun show(context: Context, analysis: AppStatisticRuleAnalysis, scope: CoroutineScope) {
    scope.launch {
      val details = analysis.definition.details ?: return@launch
      val message = buildList {
        add(details.description.resolve(context))
        analysis.resolveMatchedFacetTitles(context).takeIf(List<String>::isNotEmpty)?.let { titles ->
          add(
            buildString {
              append(context.getString(R.string.app_detail_online_rules_detected_features))
              append("\n")
              titles.forEach { title ->
                append("\n • ")
                append(title)
              }
            }
          )
        }
      }.joinToString("\n\n")

      BaseAlertDialogBuilder(context)
        .setIcon(analysis.definition.icon.resolveDrawable(context))
        .setTitle(analysis.definition.title.resolve(context))
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .setNeutralButton(R.string.lib_detail_app_props_tip) { _, _ ->
          context.openUrlInBrowser(details.referenceUrl)
        }
        .show()
    }
  }

  private fun AppStatisticRuleAnalysis.resolveMatchedFacetTitles(context: Context): List<String> {
    if (!matched) return emptyList()
    val matchedIds = matchedFacetIds.toSet()
    return definition.calculation.facets?.items
      ?.filter { facet -> facet.id in matchedIds }
      ?.map { facet -> facet.title.resolve(context) }
      .orEmpty()
  }
}
