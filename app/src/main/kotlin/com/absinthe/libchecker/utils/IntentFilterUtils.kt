package com.absinthe.libchecker.utils

import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageParser
import java.io.File

data class ParsedComponent(
  val className: String,
  val intentFilters: List<ParsedIntentFilter>
)

data class ParsedIntentFilter(
  val actions: List<String>,
  val categories: List<String>,
  val dataSchemes: List<String>
)

object IntentFilterUtils {

  fun parseComponentsFromApk(apkPath: String): List<ParsedComponent> {
    val parsedComponents = mutableListOf<ParsedComponent>()

    val parser = PackageParser()
    val pkg = parser.parsePackage(
      File(apkPath),
      PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES
    )

    val allComponents = mutableListOf<PackageParser.Component<*>>()
    allComponents.addAll(pkg.activities)
    allComponents.addAll(pkg.receivers)
    allComponents.addAll(pkg.services)

    for (component in allComponents) {
      if (component.intents.isNullOrEmpty()) continue

      val intentFilters = component.intents.map { parseIntentFilter(it) }
      parsedComponents.add(
        ParsedComponent(
          className = component.className,
          intentFilters = intentFilters
        )
      )
    }

    return parsedComponents
  }

  private fun parseIntentFilter(filter: IntentFilter): ParsedIntentFilter {
    val actions = mutableListOf<String>()
    val categories = mutableListOf<String>()
    val dataSchemes = mutableListOf<String>()

    val actionsIter = filter.actionsIterator()
    while (actionsIter.hasNext()) {
      actions.add(actionsIter.next())
    }

    val categoriesIter = filter.categoriesIterator()
    if (categoriesIter != null) {
      while (categoriesIter.hasNext()) {
        categories.add(categoriesIter.next())
      }
    }

    val count = filter.countDataSchemes()
    for (i in 0 until count) {
      filter.getDataScheme(i)?.let { dataSchemes.add(it) }
    }

    return ParsedIntentFilter(actions, categories, dataSchemes)
  }
}
