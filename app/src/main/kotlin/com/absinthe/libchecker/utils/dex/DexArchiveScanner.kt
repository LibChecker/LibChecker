package com.absinthe.libchecker.utils.dex

import android.content.pm.PackageInfo
import android.os.Trace
import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.dex.StreamingDexClassScanner
import java.io.File
import java.util.concurrent.CancellationException

object DexArchiveScanner {
  private const val TRACE_FIND_DEX_CLASSES = "LC PackageUtils findDexClasses"

  private inline fun <T> tracePackageUtilsSection(sectionName: String, block: () -> T): T {
    Trace.beginSection(sectionName)
    return try {
      block()
    } finally {
      Trace.endSection()
    }
  }

  /**
   * Check if a package contains a class
   * @param sourceFile Source file
   * @param classes Class name
   * @param hasAny true if has any class, false if has all classes
   */
  fun findDexClasses(
    sourceFile: File,
    classes: List<String>,
    hasAny: Boolean = false,
    checkCancelled: () -> Unit = {}
  ): List<String> = runCatching {
    checkCancelled()
    ZipFileCompat(sourceFile).use { findDexClasses(it, classes, hasAny, checkCancelled) }
  }.getOrElse {
    if (it is java.util.concurrent.CancellationException) throw it
    emptyList()
  }

  fun findDexClasses(
    zipFile: IZipFile,
    classes: List<String>,
    hasAny: Boolean = false,
    checkCancelled: () -> Unit = {}
  ): List<String> {
    if (classes.isEmpty()) return emptyList()
    val foundClasses = linkedSetOf<String>()
    val distinctClassCount = classes.toSet().size
    return tracePackageUtilsSection(TRACE_FIND_DEX_CLASSES) {
      runCatching {
        zipFile.getZipEntries().asSequence()
          .filter {
            checkCancelled()
            isDexEntryName(it.name)
          }
          .forEach { entry ->
            checkCancelled()
            val remainingClasses = classes.filterNot(foundClasses::contains)
            zipFile.getInputStream(entry).use { inputStream ->
              foundClasses += StreamingDexClassScanner.findClasses(
                inputStream = inputStream,
                classPatterns = remainingClasses,
                hasAny = hasAny,
                entrySize = entry.size.takeIf { it >= 0 },
                checkCancelled = checkCancelled
              )
            }
            if ((hasAny && foundClasses.isNotEmpty()) || foundClasses.size == distinctClassCount) {
              return@runCatching foundClasses.toList()
            }
          }
        foundClasses.toList()
      }.getOrElse {
        if (it is java.util.concurrent.CancellationException) throw it
        emptyList()
      }
    }
  }

  /**
   * Get part of DEX classes (at most 5 DEX's) of an app
   * @param pi PackageInfo of the app
   * @return List of LibStringItem
   */
  fun getDexList(pi: PackageInfo): Collection<LibStringItem> {
    throw RuntimeException("Not implemented")
  }

  internal fun isDexEntryName(name: String): Boolean {
    val len = name.length
    if (len < 11 || !name.startsWith("classes") || !name.endsWith(".dex")) return false
    for (i in 7 until len - 4) {
      val c = name[i]
      if (c < '0' || c > '9') return false
    }
    return true
  }
}
