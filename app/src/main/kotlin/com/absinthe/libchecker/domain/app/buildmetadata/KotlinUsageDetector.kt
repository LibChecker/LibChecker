package com.absinthe.libchecker.domain.app.buildmetadata

import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.utils.dex.DexArchiveScanner.findDexClasses
import com.absinthe.libchecker.utils.dex.DexArchiveScanner.isDexEntryName
import com.absinthe.libchecker.utils.dex.StreamingDexClassScanner
import com.absinthe.libchecker.utils.extensions.toClassDefType
import java.io.File
import java.util.concurrent.CancellationException

object KotlinUsageDetector {
  private val KOTLIN_DEX_PATTERNS = listOf(
    "kotlin.*".toClassDefType(),
    "kotlinx.*".toClassDefType()
  )

  /**
   * Check if an app uses Kotlin language from classes.dex
   * @param file APK file of the app
   * @return true if it uses Kotlin language
   */
  fun isKotlinUsedInClassDex(file: File): Boolean {
    return findDexClasses(
      file,
      KOTLIN_DEX_PATTERNS,
      hasAny = true
    ).isNotEmpty() || hasKotlinRuntimeEvidenceInClassDex(file)
  }

  /** Detects Kotlin runtime traces that remain after R8 renames and repackages class definitions. */
  internal fun hasKotlinRuntimeEvidenceInClassDex(file: File): Boolean {
    return runCatching {
      ZipFileCompat(file).use { hasKotlinRuntimeEvidenceInClassDex(it) }
    }.getOrDefault(false)
  }

  internal fun hasKotlinRuntimeEvidenceInClassDex(zipFile: IZipFile, checkCancelled: () -> Unit = {}): Boolean {
    val foundMarkers = linkedSetOf<String>()
    var remainingScanBytes = MAX_KOTLIN_RUNTIME_SCAN_BYTES
    return runCatching {
      zipFile.getZipEntries().asSequence()
        .filter {
          checkCancelled()
          isDexEntryName(it.name)
        }
        .forEach { entry ->
          checkCancelled()
          if (remainingScanBytes <= 0) return@runCatching false
          val remainingMarkers = KOTLIN_RUNTIME_ALL_MARKERS.filterNot(foundMarkers::contains)
          val scanResult = zipFile.getInputStream(entry).use { inputStream ->
            StreamingDexClassScanner.findStringMatches(
              inputStream = inputStream,
              stringPatterns = remainingMarkers,
              stopAfterMatches = remainingMarkers.size,
              entrySize = entry.size.takeIf { it >= 0 },
              maxScanBytes = remainingScanBytes,
              stopWhen = { matches -> hasSufficientKotlinRuntimeEvidence(foundMarkers + matches) },
              checkCancelled = checkCancelled
            )
          }
          remainingScanBytes -= scanResult.bytesRead
          foundMarkers += scanResult.matches
          if (hasSufficientKotlinRuntimeEvidence(foundMarkers)) {
            return@runCatching true
          }
        }
      false
    }.getOrElse {
      if (it is java.util.concurrent.CancellationException) throw it
      false
    }
  }

  private fun hasSufficientKotlinRuntimeEvidence(markers: Set<String>): Boolean {
    return KOTLIN_RUNTIME_STRING_MARKERS.count(markers::contains) >= KOTLIN_RUNTIME_STRING_THRESHOLD ||
      KOTLIN_RUNTIME_STRING_PAIRS.any { (first, second) -> first in markers && second in markers }
  }

  private const val KOTLIN_RUNTIME_STRING_THRESHOLD = 2
  private const val MAX_KOTLIN_RUNTIME_SCAN_BYTES = 32 * 1024 * 1024
  private val KOTLIN_RUNTIME_STRING_MARKERS = listOf(
    "kotlin.jvm.functions.Function1",
    "kotlin.coroutines.jvm.internal.BaseContinuationImpl",
    "kotlinx.coroutines.internal.StackTraceRecoveryKt",
    "COROUTINE_SUSPENDED"
  )

  // Exact pairs from Kotlin's reflection runtime, Intrinsics/collections and
  // suspend state machines. Keep these separate from the legacy two-marker rule:
  // unrelated messages or one legacy marker plus one new message are insufficient.
  private val KOTLIN_RUNTIME_STRING_PAIRS = listOf(
    " (Kotlin reflection is not available)" to "kotlin.reflect.jvm.internal.ReflectionFactoryImpl",
    "Parameter specified as non-null is null: method " to "Operation is not supported for read-only collection",
    "call to 'resume' before 'invoke' with coroutine" to "This continuation is already complete"
  )
  private val KOTLIN_RUNTIME_ALL_MARKERS = KOTLIN_RUNTIME_STRING_MARKERS +
    KOTLIN_RUNTIME_STRING_PAIRS.flatMap { (first, second) -> listOf(first, second) }
}
