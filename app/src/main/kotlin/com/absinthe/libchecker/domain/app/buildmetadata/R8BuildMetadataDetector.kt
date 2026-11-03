package com.absinthe.libchecker.domain.app.buildmetadata

import com.absinthe.libchecker.compat.IZipFile
import com.absinthe.libchecker.utils.dex.ZipDexContainer2
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.squareup.moshi.Moshi
import java.io.File
import kotlinx.coroutines.CancellationException

// Metadata is optional; skip oversized DEX entries instead of exhausting the app heap.
internal const val MAX_BUILD_METADATA_DEX_BYTES = 64L * 1024 * 1024

internal object R8BuildMetadataDetector {
  private const val PREFIX = "~~R8{"
  private const val MAX_MARKER_LENGTH = 16_384
  private val adapter = Moshi.Builder().build().adapter(Map::class.java)

  fun detect(
    apk: File,
    zip: IZipFile,
    onReadFailure: () -> Unit = {},
    checkCancelled: () -> Unit = {}
  ): List<String> {
    val container = ZipDexContainer2(apk, Opcodes.getDefault(), zip, MAX_BUILD_METADATA_DEX_BYTES, checkCancelled)
    val results = linkedSetOf<String>()
    try {
      container.dexEntryNames.forEach { name ->
        checkCancelled()
        try {
          container.getEntry(name)?.dexFile?.let { dex ->
            results.addAll(detect(dex, onReadFailure, checkCancelled))
          }
        } catch (e: CancellationException) {
          throw e
        } catch (_: Exception) {
          onReadFailure()
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      onReadFailure()
    }
    return results.toList()
  }

  fun detect(
    dex: DexBackedDexFile,
    onReadFailure: () -> Unit = {},
    checkCancelled: () -> Unit = {}
  ): List<String> {
    val results = linkedSetOf<String>()
    try {
      val strings = dex.stringSection
      var low = 0
      var high = strings.size
      while (low < high) {
        checkCancelled()
        val middle = low + (high - low) / 2
        if (readString(dex, middle, prefixOnly = true)!! < PREFIX) low = middle + 1 else high = middle
      }
      while (low < strings.size) {
        checkCancelled()
        if (readString(dex, low, prefixOnly = true) != PREFIX) break
        readString(dex, low++, prefixOnly = false)?.let(::parseMarker)?.let(results::add)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      onReadFailure()
    }
    return results.toList()
  }

  private fun readString(dex: DexBackedDexFile, index: Int, prefixOnly: Boolean): String? {
    val offset = dex.buffer.readSmallUint(dex.stringSection.getOffset(index))
    val buffer = dex.dataBuffer
    val reader = buffer.readerAt(offset)
    val length = reader.readSmallUleb128()
    // dexlib allocates a thread-local char array from this untrusted length before decoding.
    val remaining = buffer.buf.size.toLong() - buffer.baseOffset - reader.offset
    require(length.toLong() < remaining) { "DEX string length exceeds available data" }
    if (!prefixOnly && length > MAX_MARKER_LENGTH) return null
    val value = reader.readString(if (prefixOnly) minOf(length, PREFIX.length) else length)
    if (!prefixOnly) require(reader.readUbyte() == 0) { "Unterminated DEX string" }
    return value
  }

  internal fun parseMarker(string: String): String? {
    if (!string.startsWith(PREFIX) || string.length > MAX_MARKER_LENGTH) return null
    return try {
      val fields = adapter.fromJson(string.substring(4)) ?: return null
      val version = (fields["version"] as? String)?.takeIf {
        it.length <= 128 && it.matches(Regex("[0-9][A-Za-z0-9._+\\-]*"))
      } ?: return null
      when (fields["r8-mode"]) {
        "full" -> "$version (Full)"
        "compatibility" -> "$version (Compatibility)"
        else -> version
      }
    } catch (_: Exception) {
      null
    }
  }
}
