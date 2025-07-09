package com.absinthe.libchecker.utils

import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageParser
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import java.io.File

data class ParsedComponent(
  @LibType val type: Int,
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

    val allComponents = mutableListOf<Pair<Int, List<PackageParser.Component<*>>>>()
    allComponents.add(ACTIVITY to pkg.activities)
    allComponents.add(RECEIVER to pkg.receivers)
    allComponents.add(SERVICE to pkg.services)

    allComponents.forEach {
      val (type, components) = it
      components.forEach { component ->
        if (component.intents.isNullOrEmpty()) return@forEach

        val intentFilters = component.intents.map { parseIntentFilter(it) }
        parsedComponents.add(
          ParsedComponent(
            type = type,
            className = component.className,
            intentFilters = intentFilters
          )
        )
      }
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
