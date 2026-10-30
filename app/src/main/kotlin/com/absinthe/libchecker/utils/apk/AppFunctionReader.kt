package com.absinthe.libchecker.utils.apk

import android.content.pm.PackageInfo
import android.util.Xml
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.domain.app.model.AppFunctionInfo
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import org.xmlpull.v1.XmlPullParser
import pxb.android.Res_value
import pxb.android.axml.AxmlReader
import pxb.android.axml.AxmlVisitor
import pxb.android.axml.NodeVisitor

/** Reads declarations rather than the privileged, device-specific AppFunction registry. */
internal object AppFunctionReader {
  fun read(packageInfo: PackageInfo): List<AppFunctionInfo> {
    val app = packageInfo.applicationInfo ?: return emptyList()
    return try {
      readPaths((listOfNotNull(app.sourceDir) + app.splitSourceDirs.orEmpty()).distinct())
    } catch (_: ScanLimitExceeded) {
      emptyList()
    }
  }

  private fun readPaths(paths: List<String>): List<AppFunctionInfo> {
    val budget = ReadBudget()
    val declarations = linkedSetOf<String>()
    for (path in paths) {
      readOptional {
        ZipFileCompat(path).use { zip ->
          val manifest = zip.getEntry("AndroidManifest.xml") ?: return@use
          val bytes = zip.getInputStream(manifest).use { budget.read(it) }
          declarations.addAll(readAssetNames(bytes))
          checkLimit(declarations.size <= MAX_ASSETS)
        }
      }
    }
    if (declarations.isEmpty()) return emptyList()
    val functions = linkedMapOf<String, AppFunctionInfo>()
    for (path in paths) {
      readOptional {
        ZipFileCompat(path).use { zip ->
          for (asset in declarations) {
            readOptional asset@{
              val entry = zip.getEntry("assets/$asset") ?: return@asset
              val bytes = zip.getInputStream(entry).use { budget.read(it, MAX_XML_BYTES) }
              val parsed = readFunctions(bytes, functions)
              functions.clear()
              parsed.associateByTo(functions) { it.id }
            }
          }
        }
      }
    }
    return functions.values.toList()
  }

  internal fun readAssetNames(manifest: ByteArray): List<String> {
    val assets = mutableListOf<String>()
    fun propertyVisitor() = object : NodeVisitor() {
      var propertyName: String? = null
      var asset: String? = null

      override fun attr(ns: String?, name: String?, resourceId: Int, raw: String?, value: Res_value) {
        if (ns != "http://schemas.android.com/apk/res/android" || value.type != Res_value.TYPE_STRING) return
        when (name) {
          "name" -> propertyName = value.toString()
          "value" -> asset = value.toString()
        }
      }

      override fun end() {
        if (propertyName == "android.app.appfunctions" || propertyName == "android.app.appfunctions.v2") {
          asset?.splitToSequence(',')?.map(String::trim)?.filter {
            it.isNotBlank() && !it.startsWith('/') && it.split('/').none { part -> part == ".." }
          }?.forEach {
            checkLimit(it.length <= 1024 && assets.size < MAX_ASSETS)
            assets.add(it)
          }
        }
      }
    }
    AxmlReader(manifest).accept(object : AxmlVisitor() {
      override fun child(ns: String?, name: String?): NodeVisitor? {
        if (ns != null || name != "manifest") return null
        return object : NodeVisitor() {
          override fun child(ns: String?, name: String?): NodeVisitor? {
            if (ns != null || name != "application") return null
            return object : NodeVisitor() {
              override fun child(ns: String?, name: String?): NodeVisitor? {
                if (ns != null) return null
                if (name == "property") return propertyVisitor()
                if (name != "service") return null
                return object : NodeVisitor() {
                  override fun child(ns: String?, name: String?): NodeVisitor? {
                    return if (ns == null && name == "property") propertyVisitor() else null
                  }
                }
              }
            }
          }
        }
      }
    })
    return assets.distinct()
  }

  internal fun readFunctions(bytes: ByteArray, existing: Map<String, AppFunctionInfo> = emptyMap()): List<AppFunctionInfo> {
    checkLimit(bytes.size <= MAX_XML_BYTES)
    val parser = Xml.newPullParser()
    parser.setInput(bytes.inputStream(), null)
    val functions = LinkedHashMap(existing)
    var inFunction = false
    var id: String? = null
    var name: String? = null
    var description: String? = null
    var rootClosed = false
    while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
      require(parser.eventType != XmlPullParser.DOCDECL) { "AppFunction metadata must not contain a DTD" }
      if (parser.eventType == XmlPullParser.END_TAG && parser.depth == 1) rootClosed = true
      if (parser.eventType == XmlPullParser.END_TAG && parser.depth == 2 && inFunction) {
        id?.let { functionId ->
          val previous = functions[functionId]
          checkLimit(functionId.length <= 1024 && (name?.length ?: 0) <= 512 && (description?.length ?: 0) <= 4096)
          functions[functionId] = AppFunctionInfo(
            id = functionId,
            name = name ?: previous?.name ?: functionId.substringAfterLast('#'),
            description = description ?: previous?.description
          )
          checkFunctions(functions.values)
        }
        inFunction = false
      }
      if (parser.eventType == XmlPullParser.START_TAG) {
        when (parser.depth) {
          1 -> require(parser.name == "appfunctions")

          2 -> {
            inFunction = parser.name == "appfunction"
            id = null
            name = null
            description = null
          }

          3 -> if (inFunction) {
            when (parser.name) {
              "functionId", "function_id", "id" -> id = parser.nextText().trim().takeIf(String::isNotEmpty) ?: id
              "name" -> name = parser.nextText().trim().takeIf(String::isNotEmpty)
              "description" -> description = parser.nextText().trim().takeIf(String::isNotEmpty)
            }
          }
        }
      }
    }
    require(rootClosed) { "Incomplete AppFunction metadata" }
    return functions.values.toList()
  }

  // Bound both decompression work and the text/icons later rendered on the main thread.
  private const val MAX_ASSETS = 64
  private const val MAX_XML_BYTES = 512 * 1024
  private const val MAX_SCAN_BYTES = 8 * 1024 * 1024

  private fun checkFunctions(functions: Collection<AppFunctionInfo>) {
    checkLimit(functions.size <= 128)
    checkLimit(functions.sumOf { it.id.length + it.name.length + (it.description?.length ?: 0) } <= 64 * 1024)
  }

  internal class ReadBudget {
    private var remaining = MAX_SCAN_BYTES

    fun read(input: InputStream, entryLimit: Int = MAX_SCAN_BYTES): ByteArray {
      val output = ByteArrayOutputStream()
      val buffer = ByteArray(8192)
      while (true) {
        val available = minOf(entryLimit - output.size(), remaining)
        val count = input.read(buffer, 0, minOf(buffer.size, available + 1))
        if (count == -1) return output.toByteArray()
        checkLimit(count <= available)
        remaining -= count
        output.write(buffer, 0, count)
      }
    }
  }

  private class ScanLimitExceeded : IllegalArgumentException("AppFunction metadata exceeds scan limits")

  private fun checkLimit(condition: Boolean) {
    if (!condition) throw ScanLimitExceeded()
  }

  private inline fun readOptional(block: () -> Unit) {
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: ScanLimitExceeded) {
      throw e
    } catch (_: Exception) {
      // Missing or malformed metadata must not prevent other detail features from loading.
    }
  }
}
