package com.absinthe.libchecker.utils

import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageParser
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.compat.ZipFileCompat
import java.io.File
import pxb.android.Res_value
import pxb.android.axml.AxmlReader
import pxb.android.axml.AxmlVisitor
import pxb.android.axml.NodeVisitor
import timber.log.Timber

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
    return runCatching {
      parseComponentsFromManifest(apkPath)
    }.onFailure {
      Timber.w(it, "Failed to parse intent filters from manifest: $apkPath")
    }.getOrElse {
      parseComponentsWithPackageParser(apkPath)
    }
  }

  private fun parseComponentsFromManifest(apkPath: String): List<ParsedComponent> {
    val parsedComponents = mutableListOf<ParsedComponent>()
    ZipFileCompat(File(apkPath)).use { zip ->
      val entry = zip.getEntry(MANIFEST_ENTRY_NAME) ?: return emptyList()
      val bytes = zip.getInputStream(entry).use { it.readBytes() }
      AxmlReader(bytes).accept(object : AxmlVisitor() {
        override fun child(ns: String?, name: String?): NodeVisitor {
          val child = super.child(ns, name)
          return if (name == TAG_MANIFEST) {
            ManifestTagVisitor(child, parsedComponents)
          } else {
            child
          }
        }
      })
    }
    return parsedComponents
  }

  private fun parseComponentsWithPackageParser(apkPath: String): List<ParsedComponent> {
    val parsedComponents = mutableListOf<ParsedComponent>()

    val parser = PackageParser()
    val pkg = runCatching {
      parser.parsePackage(
        File(apkPath),
        PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES
      )
    }.onFailure {
      Timber.w(it, "Failed to parse intent filters from APK: $apkPath")
    }.getOrNull() ?: return emptyList()

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

  private class ManifestTagVisitor(
    child: NodeVisitor,
    private val parsedComponents: MutableList<ParsedComponent>
  ) : NodeVisitor(child) {
    private var packageName = ""

    override fun attr(ns: String?, name: String?, resourceId: Int, raw: String?, value: Res_value?) {
      if (name == ATTR_PACKAGE) {
        packageName = value.asString(raw).orEmpty()
      }
      super.attr(ns, name, resourceId, raw, value)
    }

    override fun child(ns: String?, name: String?): NodeVisitor {
      val child = super.child(ns, name)
      return if (name == TAG_APPLICATION) {
        ApplicationTagVisitor(child, parsedComponents) { packageName }
      } else {
        child
      }
    }
  }

  private class ApplicationTagVisitor(
    child: NodeVisitor,
    private val parsedComponents: MutableList<ParsedComponent>,
    private val packageName: () -> String
  ) : NodeVisitor(child) {

    override fun child(ns: String?, name: String?): NodeVisitor {
      val child = super.child(ns, name)
      return when (name) {
        TAG_SERVICE -> ComponentTagVisitor(child, SERVICE, parsedComponents, packageName)
        TAG_ACTIVITY, TAG_ACTIVITY_ALIAS -> ComponentTagVisitor(child, ACTIVITY, parsedComponents, packageName)
        TAG_RECEIVER -> ComponentTagVisitor(child, RECEIVER, parsedComponents, packageName)
        TAG_PROVIDER -> ComponentTagVisitor(child, PROVIDER, parsedComponents, packageName)
        else -> child
      }
    }
  }

  private class ComponentTagVisitor(
    child: NodeVisitor,
    @LibType private val type: Int,
    private val parsedComponents: MutableList<ParsedComponent>,
    private val packageName: () -> String
  ) : NodeVisitor(child) {
    private var className: String? = null
    private val intentFilters = mutableListOf<ParsedIntentFilter>()

    override fun attr(ns: String?, name: String?, resourceId: Int, raw: String?, value: Res_value?) {
      if (name == ATTR_NAME) {
        className = value.asString(raw)
      }
      super.attr(ns, name, resourceId, raw, value)
    }

    override fun child(ns: String?, name: String?): NodeVisitor {
      val child = super.child(ns, name)
      return if (name == TAG_INTENT_FILTER) {
        IntentFilterTagVisitor(child, intentFilters)
      } else {
        child
      }
    }

    override fun end() {
      val normalizedClassName = normalizeClassName(packageName(), className)
      if (normalizedClassName != null && intentFilters.isNotEmpty()) {
        parsedComponents.add(
          ParsedComponent(
            type = type,
            className = normalizedClassName,
            intentFilters = intentFilters.toList()
          )
        )
      }
      super.end()
    }
  }

  private class IntentFilterTagVisitor(
    child: NodeVisitor,
    private val intentFilters: MutableList<ParsedIntentFilter>
  ) : NodeVisitor(child) {
    private val actions = mutableListOf<String>()
    private val categories = mutableListOf<String>()
    private val dataSchemes = mutableListOf<String>()

    override fun child(ns: String?, name: String?): NodeVisitor {
      val child = super.child(ns, name)
      return when (name) {
        TAG_ACTION -> NamedValueTagVisitor(child, actions)
        TAG_CATEGORY -> NamedValueTagVisitor(child, categories)
        TAG_DATA -> DataTagVisitor(child, dataSchemes)
        else -> child
      }
    }

    override fun end() {
      intentFilters.add(
        ParsedIntentFilter(
          actions = actions.toList(),
          categories = categories.toList(),
          dataSchemes = dataSchemes.toList()
        )
      )
      super.end()
    }
  }

  private class NamedValueTagVisitor(
    child: NodeVisitor,
    private val values: MutableList<String>
  ) : NodeVisitor(child) {

    override fun attr(ns: String?, name: String?, resourceId: Int, raw: String?, value: Res_value?) {
      if (name == ATTR_NAME) {
        value.asString(raw)?.let(values::add)
      }
      super.attr(ns, name, resourceId, raw, value)
    }
  }

  private class DataTagVisitor(
    child: NodeVisitor,
    private val dataSchemes: MutableList<String>
  ) : NodeVisitor(child) {

    override fun attr(ns: String?, name: String?, resourceId: Int, raw: String?, value: Res_value?) {
      if (name == ATTR_SCHEME) {
        value.asString(raw)?.let(dataSchemes::add)
      }
      super.attr(ns, name, resourceId, raw, value)
    }
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

  private fun normalizeClassName(packageName: String, className: String?): String? {
    if (className.isNullOrBlank()) {
      return null
    }
    return when {
      className.startsWith(".") -> packageName + className
      "." in className -> className
      packageName.isNotBlank() -> "$packageName.$className"
      else -> className
    }
  }

  private fun Res_value?.asString(raw: String?): String? {
    if (this == null || type == Res_value.TYPE_NULL) {
      return raw
    }
    return if (type == Res_value.TYPE_STRING) {
      toString()
    } else {
      raw ?: toString()
    }
  }

  private const val MANIFEST_ENTRY_NAME = "AndroidManifest.xml"
  private const val TAG_MANIFEST = "manifest"
  private const val TAG_APPLICATION = "application"
  private const val TAG_SERVICE = "service"
  private const val TAG_ACTIVITY = "activity"
  private const val TAG_ACTIVITY_ALIAS = "activity-alias"
  private const val TAG_RECEIVER = "receiver"
  private const val TAG_PROVIDER = "provider"
  private const val TAG_INTENT_FILTER = "intent-filter"
  private const val TAG_ACTION = "action"
  private const val TAG_CATEGORY = "category"
  private const val TAG_DATA = "data"
  private const val ATTR_PACKAGE = "package"
  private const val ATTR_NAME = "name"
  private const val ATTR_SCHEME = "scheme"
}
