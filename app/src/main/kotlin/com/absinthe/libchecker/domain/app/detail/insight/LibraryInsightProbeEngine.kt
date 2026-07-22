package com.absinthe.libchecker.domain.app.detail.insight

import android.content.pm.PackageInfo
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.utils.PackageUtils
import java.io.File
import java.io.InputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class LibraryInsightProbeEngine {

  suspend fun probe(
    packageInfo: PackageInfo,
    definition: LibraryInsightDefinition
  ): LibraryInsightProbeResult = withContext(Dispatchers.IO) {
    val values = linkedMapOf<String, LinkedHashSet<String>>()
    var evidenceFound = false

    definition.probes.forEach { probe ->
      coroutineContext.ensureActive()
      val probeContext = coroutineContext
      val captures = probe.captures.associateWith { values.getOrPut(it.output, ::linkedSetOf) }
      var remainingTotal = probe.reader.maxTotalBytes

      fun scan(input: InputStream) {
        if (remainingTotal <= 0 || captures.all { (capture, output) -> output.size >= capture.maxResults }) return
        val limit = minOf(probe.reader.maxBytesPerFile, remainingTotal)
        val consumed = scanAsciiStrings(input, limit, probeContext) { token ->
          captures.forEach { (capture, output) ->
            if (output.size < capture.maxResults) {
              capture.findValue(token)?.let(output::add)
            }
          }
        }
        remainingTotal -= consumed
      }

      val directFile = packageInfo.applicationInfo?.nativeLibraryDir
        ?.let(::File)
        ?.resolve(probe.source.fileName)
        ?.takeIf { it.isFile && it.canRead() }
      var directSourceScanned = false
      if (directFile != null) {
        evidenceFound = true
        try {
          directFile.inputStream().buffered().use(::scan)
          directSourceScanned = true
        } catch (exception: CancellationException) {
          throw exception
        } catch (_: Exception) {
          // Fall back to the packaged copy below.
        }
      }

      if (
        !directSourceScanned &&
        remainingTotal > 0 &&
        captures.any { (capture, output) -> output.size < capture.maxResults }
      ) {
        for (archive in packageArchiveFiles(packageInfo)) {
          if (remainingTotal <= 0 || captures.all { (capture, output) -> output.size >= capture.maxResults }) {
            break
          }
          try {
            ZipFileCompat(archive).use { zip ->
              for (path in probe.source.archivePaths) {
                val entry = zip.getEntry(path) ?: continue
                evidenceFound = true
                zip.getInputStream(entry).buffered().use(::scan)
              }
            }
          } catch (exception: CancellationException) {
            throw exception
          } catch (_: Exception) {
            // A broken split must not prevent checking the remaining package files.
          }
        }
      }
    }

    LibraryInsightProbeResult(
      evidenceFound = evidenceFound,
      values = values.mapValues { it.value.toList() }
    )
  }

  private fun scanAsciiStrings(
    input: InputStream,
    limit: Long,
    coroutineContext: CoroutineContext,
    onToken: (String) -> Unit
  ): Long {
    val buffer = ByteArray(BUFFER_SIZE)
    val token = StringBuilder()
    var discardToken = false
    var consumed = 0L

    fun flushToken() {
      if (!discardToken && token.length >= MIN_TOKEN_LENGTH) onToken(token.toString())
      token.setLength(0)
      discardToken = false
    }

    while (consumed < limit) {
      coroutineContext.ensureActive()
      val count = input.read(buffer, 0, minOf(buffer.size.toLong(), limit - consumed).toInt())
      if (count < 0) break
      consumed += count
      for (index in 0 until count) {
        val value = buffer[index].toInt() and 0xff
        if (value in PRINTABLE_ASCII_RANGE) {
          if (!discardToken) {
            if (token.length < LibraryInsightDefinitionValidator.MAX_TOKEN_LENGTH) {
              token.append(value.toChar())
            } else {
              token.setLength(0)
              discardToken = true
            }
          }
        } else {
          flushToken()
        }
      }
    }
    flushToken()
    return consumed
  }

  private fun LibraryInsightDefinition.Capture.findValue(token: String): String? {
    return when (type) {
      LibraryInsightDefinitionValidator.CAPTURE_SHA1 -> SHA1.find(token)?.value?.lowercase()
      LibraryInsightDefinitionValidator.CAPTURE_SEMVER_CHANNEL -> SEMVER_CHANNEL.find(token)?.groupValues?.get(1)
      else -> null
    }
  }

  private fun packageArchiveFiles(packageInfo: PackageInfo): List<File> {
    val applicationInfo = packageInfo.applicationInfo ?: return emptyList()
    return buildList {
      applicationInfo.sourceDir?.let(::File)?.takeIf(File::isFile)?.let(::add)
      PackageUtils.getSplitsSourceDir(packageInfo)
        .orEmpty()
        .map(::File)
        .filter(File::isFile)
        .forEach(::add)
    }.distinctBy(File::getAbsolutePath)
  }

  private companion object {
    const val BUFFER_SIZE = 32 * 1024
    const val MIN_TOKEN_LENGTH = 4
    val PRINTABLE_ASCII_RANGE = 0x20..0x7e
    val SHA1 = Regex("(?i)(?<![0-9a-f])[0-9a-f]{40}(?![0-9a-f])")
    val SEMVER_CHANNEL = Regex("(?<![0-9A-Za-z])([0-9]+\\.[0-9]+\\.[0-9]+(?:-[0-9A-Za-z.-]+)?)\\s+\\((?:stable|beta|dev|main)\\)")
  }
}
