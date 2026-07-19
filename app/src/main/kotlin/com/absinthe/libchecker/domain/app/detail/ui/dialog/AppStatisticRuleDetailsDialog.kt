package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.statistics.AppStatisticRuleAnalysis
import com.absinthe.libchecker.domain.statistics.chart.ui.resolve
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.Toasty
import timber.log.Timber

object AppStatisticRuleDetailsDialog {

  fun show(context: Context, analysis: AppStatisticRuleAnalysis) {
    val details = analysis.definition.details ?: return
    val message = buildList {
      add(details.description.resolve(context))
      analysis.resolveMatchedFacetTitles(context).takeIf(List<String>::isNotEmpty)?.let { titles ->
        add(
          buildString {
            append(context.getString(R.string.app_detail_online_rules_detected_features))
            titles.forEach { title ->
              append("\n • ")
              append(title)
            }
          }
        )
      }
    }.joinToString("\n\n")

    BaseAlertDialogBuilder(context)
      .setTitle(analysis.definition.title.resolve(context))
      .setMessage(message)
      .setPositiveButton(android.R.string.ok, null)
      .setNeutralButton(R.string.lib_detail_app_props_tip) { _, _ ->
        openReference(context, details.referenceUrl)
      }
      .show()
  }

  private fun AppStatisticRuleAnalysis.resolveMatchedFacetTitles(context: Context): List<String> {
    if (!matched) return emptyList()
    val matchedIds = matchedFacetIds.toSet()
    return definition.calculation.facets?.items
      ?.filter { facet -> facet.id in matchedIds }
      ?.map { facet -> facet.title.resolve(context) }
      .orEmpty()
  }

  private fun openReference(context: Context, url: String) {
    runCatching {
      CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
    }.onFailure { error ->
      Timber.e(error)
      runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
      }.onFailure { fallbackError ->
        Timber.e(fallbackError)
        Toasty.showShort(context, "No browser application")
      }
    }
  }
}
