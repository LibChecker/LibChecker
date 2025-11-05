package com.absinthe.libchecker.utils.apk

import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.utils.extensions.STRING_ABI_MAP
import com.absinthe.libchecker.utils.manifest.FullManifestReader
import com.absinthe.libraries.utils.manager.TimeRecorder
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import timber.log.Timber

class ApkPreview(val url: String) {
  private val client = ApiManager.okHttpClient
  private val httpUrl = url.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL: $url")
  private val elfMap: MutableMap<Int, MutableList<Pair<String, Int>>> = mutableMapOf()

  fun parse(): Result<ApkPreviewInfo> = runCatching {
    val recorder = TimeRecorder().apply { start() }
    val metadata = fetchMetadata()

    val manifestBytes = try {
      if (metadata.supportsRange && metadata.contentLength > 0) {
        fetchManifestWithRanges(metadata.contentLength)
      } else {
        fetchManifestWithFullArchive()
      }
    } catch (e: RangeNotSupportedException) {
      Timber.w(e, "Falling back to full download for %s", url)
      fetchManifestWithFullArchive()
    }

    @Suppress("UNCHECKED_CAST")
    val manifestReader = FullManifestReader(manifestBytes, null)
    val manifestProperties = manifestReader.properties as Map<String, Any?>

    recorder.end()
    Timber.d("Parsed manifest preview from %s in %s", url, recorder)
    Timber.d("Parsed manifest preview  %s", manifestProperties["minSdkVersion"])

    ApkPreviewInfo(
      packageName = (manifestProperties["package"] as? String).orEmpty(),
      versionCode = (manifestProperties["versionCode"] as? String)?.toLong() ?: -1L,
      versionName = (manifestProperties["versionName"] as? String).orEmpty(),
      compileSdkVersion = (manifestProperties["compileSdkVersion"] as? String)?.toInt() ?: -1,
      targetSdkVersion = (manifestProperties["targetSdkVersion"] as? String)?.toInt() ?: -1,
      minSdkVersion = (manifestProperties["minSdkVersion"] as? String)?.toInt() ?: -1,
      packageSize = metadata.contentLength,
      abiSet = elfMap.keys,
      appProps = manifestReader.properties.map { it -> it.key to (it.value?.toString() ?: "") }.toMap(),
      nativeLibs = elfMap,
      services = manifestReader.services,
      activities = manifestReader.activities,
      receivers = manifestReader.receivers,
      providers = manifestReader.providers,
      permissions = manifestReader.permissionList,
      metadata = manifestReader.metadata
    )
  }

  data class EocdInfo(
    val centralDirectorySize: Long,
    val centralDirectoryOffset: Long,
    val totalEntries: Int,
    val comment: String
  )

  private fun parseEocd(eocdBytes: ByteArray): EocdInfo {
    val buffer = ByteBuffer.wrap(eocdBytes).order(ByteOrder.LITTLE_ENDIAN)

    val signature = buffer.int
    require(signature == EOCD_SIGNATURE_INT) { "Invalid EOCD signature: 0x${signature.toString(16)}" }

    buffer.short // disk number
    buffer.short // start disk number
    buffer.short // entries on this disk
    val totalEntries = buffer.short.toInt() and 0xFFFF
    val cdSize = buffer.int.toLong() and 0xFFFFFFFFL
    val cdOffset = buffer.int.toLong() and 0xFFFFFFFFL
    val commentLength = buffer.short.toInt() and 0xFFFF

    val commentBytes = ByteArray(commentLength)
    if (commentLength > 0) buffer.get(commentBytes)

    val comment = commentBytes.toString(Charsets.UTF_8)

    return EocdInfo(
      centralDirectorySize = cdSize,
      centralDirectoryOffset = cdOffset,
      totalEntries = totalEntries,
      comment = comment
    )
  }

  private fun trimToEocd(bytes: ByteArray): ByteArray? {
    for (i in 0..bytes.size - EOCD_SIGNATURE.size) {
      var matched = true
      for (j in EOCD_SIGNATURE.indices) {
        if (bytes[i + j] != EOCD_SIGNATURE[j]) {
          matched = false
          break
        }
      }
      if (matched) {
        Timber.d("EOCD signature found at offset %d", i)
        return bytes.copyOfRange(i, bytes.size)
      }
    }

    Timber.w("EOCD signature not found in provided bytes")
    return null
  }

  private data class FileMetadata(val contentLength: Long, val supportsRange: Boolean)

  private sealed class TailDownload {
    data class Partial(val bytes: ByteArray) : TailDownload()
    data class Full(val bytes: ByteArray) : TailDownload()
  }

  private data class CdEntry(
    val name: String,
    val localHeaderOffset: Long,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val compressionMethod: Int,
    val usesUtf8: Boolean,
    val rawExtra: ByteArray
  )

  private fun parseCentralDirectory(cdBytes: ByteArray): List<CdEntry> {
    val entries = mutableListOf<CdEntry>()
    var index = 0

    fun remainingFrom(i: Int) = cdBytes.size - i

    while (index + 4 <= cdBytes.size) {
      if (cdBytes[index] != 0x50.toByte() ||
        cdBytes[index + 1] != 0x4B.toByte() ||
        cdBytes[index + 2] != 0x01.toByte() ||
        cdBytes[index + 3] != 0x02.toByte()
      ) {
        index++
        continue
      }

      if (remainingFrom(index) < CENTRAL_DIRECTORY_FIXED_HEADER_SIZE) break

      val buf = ByteBuffer.wrap(cdBytes, index, cdBytes.size - index).order(ByteOrder.LITTLE_ENDIAN)

      val signature = buf.int
      if (signature != CENTRAL_DIRECTORY_SIGNATURE_INT) {
        index++
        continue
      }

      buf.short // versionMadeBy
      buf.short // versionNeeded
      val generalPurposeBitFlag = buf.short.toInt() and 0xFFFF
      val compressionMethod = buf.short.toInt() and 0xFFFF
      buf.short // lastModTime
      buf.short // lastModDate

      buf.int // crc32
      var compressedSize = buf.int.toLong() and 0xFFFFFFFFL
      var uncompressedSize = buf.int.toLong() and 0xFFFFFFFFL

      val fileNameLength = buf.short.toInt() and 0xFFFF
      val extraFieldLength = buf.short.toInt() and 0xFFFF
      val fileCommentLength = buf.short.toInt() and 0xFFFF

      buf.short // diskNumberStart
      buf.short // internalFileAttr
      buf.int // externalFileAttr
      var localHeaderOffset = buf.int.toLong() and 0xFFFFFFFFL

      val totalEntryLen =
        CENTRAL_DIRECTORY_FIXED_HEADER_SIZE + fileNameLength + extraFieldLength + fileCommentLength
      if (remainingFrom(index) < totalEntryLen) {
        break
      }

      val nameStart = index + CENTRAL_DIRECTORY_FIXED_HEADER_SIZE
      val name = if (fileNameLength > 0) {
        val rawName = cdBytes.copyOfRange(nameStart, nameStart + fileNameLength)
        val useUtf8 = (generalPurposeBitFlag and (1 shl 11)) != 0
        try {
          if (useUtf8) {
            String(rawName, Charsets.UTF_8)
          } else {
            // Legacy ZIP archives usually use CP437 (IBM437)
            String(rawName, Charset.forName("Cp437"))
          }
        } catch (_: Exception) {
          // Fallback to UTF-8
          String(rawName, Charsets.UTF_8)
        }
      } else {
        ""
      }

      val extraStart = nameStart + fileNameLength
      val rawExtra = if (extraFieldLength > 0) {
        cdBytes.copyOfRange(extraStart, extraStart + extraFieldLength)
      } else {
        ByteArray(0)
      }

      if (compressedSize == 0xFFFFFFFFL || uncompressedSize == 0xFFFFFFFFL || localHeaderOffset == 0xFFFFFFFFL) {
        var exIndex = 0
        while (exIndex + 4 <= rawExtra.size) {
          val headerId =
            (rawExtra[exIndex].toInt() and 0xFF) or ((rawExtra[exIndex + 1].toInt() and 0xFF) shl 8)
          val dataSize =
            (rawExtra[exIndex + 2].toInt() and 0xFF) or ((rawExtra[exIndex + 3].toInt() and 0xFF) shl 8)
          val dataStart = exIndex + 4
          if (dataStart + dataSize > rawExtra.size) break

          if (headerId == 0x0001) {
            val zip64Buf =
              ByteBuffer.wrap(rawExtra, dataStart, dataSize).order(ByteOrder.LITTLE_ENDIAN)
            try {
              var ptr = 0
              if (uncompressedSize == 0xFFFFFFFFL && ptr + 8 <= dataSize) {
                uncompressedSize = zip64Buf.long
                ptr += 8
              }
              if (compressedSize == 0xFFFFFFFFL && ptr + 8 <= dataSize) {
                compressedSize = zip64Buf.long
                ptr += 8
              }
              if (localHeaderOffset == 0xFFFFFFFFL && ptr + 8 <= dataSize) {
                localHeaderOffset = zip64Buf.long
                ptr += 8
              }
            } catch (_: Exception) {
            }
          }

          exIndex += 4 + dataSize
        }
      }

      val usesUtf8 = (generalPurposeBitFlag and (1 shl 11)) != 0

      entries.add(
        CdEntry(
          name = name,
          localHeaderOffset = localHeaderOffset,
          compressedSize = compressedSize,
          uncompressedSize = uncompressedSize,
          compressionMethod = compressionMethod,
          usesUtf8 = usesUtf8,
          rawExtra = rawExtra
        )
      )

      index += totalEntryLen
    }

    return entries
  }

  data class LocalHeaderInfo(
    val nameLength: Int,
    val extraLength: Int,
    val compressionMethod: Int
  )

  private fun readLocalHeader(offset: Long): LocalHeaderInfo {
    val bytes = downloadRange(offset, LOCAL_HEADER_PROBE_BYTES)

    val sigIndex = bytes.indexOfSequence(LOCAL_FILE_HEADER_SIGNATURE)
    require(sigIndex >= 0) { "No local header signature found near offset $offset" }

    val buf = ByteBuffer.wrap(bytes, sigIndex, bytes.size - sigIndex).order(ByteOrder.LITTLE_ENDIAN)
    buf.int
    buf.short
    buf.short
    val method = buf.short.toInt() and 0xFFFF
    buf.short
    buf.short
    buf.int
    buf.int
    buf.int
    val nameLen = buf.short.toInt() and 0xFFFF
    val extraLen = buf.short.toInt() and 0xFFFF

    Timber.d(
      "Local header parsed at offset=%d (nameLen=%d, extraLen=%d, method=%d)",
      offset,
      nameLen,
      extraLen,
      method
    )

    return LocalHeaderInfo(nameLen, extraLen, method)
  }

  private fun downloadEntryWithRanges(entry: CdEntry): ByteArray {
    Timber.d("Downloading entry %s", entry.name)

    val localHeader = readLocalHeader(entry.localHeaderOffset)
    val dataStart =
      entry.localHeaderOffset + LOCAL_FILE_HEADER_FIXED_SIZE + localHeader.nameLength + localHeader.extraLength
    val compressedData = downloadRange(dataStart, entry.compressedSize)

    val result = decompressEntry(compressedData, localHeader.compressionMethod)
    Timber.d("Decompressed size for %s: %d bytes", entry.name, result.size)
    return result
  }

  private fun decompressEntry(compressedData: ByteArray, method: Int): ByteArray = when (method) {
    0 -> compressedData
    8 -> if (compressedData.isEmpty()) {
      ByteArray(0)
    } else {
      InflaterInputStream(compressedData.inputStream(), Inflater(true)).use { it.readBytes() }
    }

    else -> error("Unsupported compression method: $method")
  }

  private fun fetchMetadata(): FileMetadata {
    val headRequest = newRequestBuilder().head().build()
    val headResult = runCatching { executeRequest(headRequest) }.getOrNull()

    headResult?.use { response ->
      if (response.isSuccessful) {
        val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
        val supportsRange =
          response.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true
        return FileMetadata(contentLength, supportsRange)
      }
      if (response.code != HttpURLConnection.HTTP_BAD_METHOD && response.code != HttpURLConnection.HTTP_NOT_IMPLEMENTED) {
        error("Failed to fetch metadata for $url with HEAD: ${response.code}")
      }
    }

    executeRequest(newRequestBuilder().get().build()).use { response ->
      if (!response.isSuccessful) {
        error("Metadata request failed: ${response.code}")
      }
      val contentLength = response.body.contentLength()
      val supportsRange =
        response.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true
      return FileMetadata(contentLength, supportsRange)
    }
  }

  private fun fetchManifestWithRanges(contentLength: Long): ByteArray {
    val rangeStart = (contentLength - EOCD_PROBE_BYTES).coerceAtLeast(0)
    return when (val tail = downloadArchiveTail(rangeStart)) {
      is TailDownload.Partial -> {
        val eocdBytes = trimToEocd(tail.bytes)
          ?: throw RangeNotSupportedException("EOCD signature not found in tail region")
        val eocd = parseEocd(eocdBytes)
        Timber.d(
          "EOCD located via range: offset=%d size=%d entries=%d",
          eocd.centralDirectoryOffset,
          eocd.centralDirectorySize,
          eocd.totalEntries
        )

        val cdBytes = downloadRange(eocd.centralDirectoryOffset, eocd.centralDirectorySize)
        val entries = parseCentralDirectory(cdBytes)
        val entry = entries.firstOrNull { it.name == MANIFEST_ENTRY_NAME }
          ?: error("Target entry $MANIFEST_ENTRY_NAME not found")

        parseElfFiles(entries)

        downloadEntryWithRanges(entry)
      }

      is TailDownload.Full -> processArchiveBytes(tail.bytes)
    }
  }

  private fun fetchManifestWithFullArchive(): ByteArray {
    val archive = downloadEntireArchive()
    return processArchiveBytes(archive)
  }

  private fun processArchiveBytes(archive: ByteArray): ByteArray {
    val eocdBytes = trimToEocd(archive) ?: error("EOCD signature not found")
    val eocd = parseEocd(eocdBytes)
    val cdStart = eocd.centralDirectoryOffset.toInt()
    val cdEnd = (eocd.centralDirectoryOffset + eocd.centralDirectorySize).toInt()
    require(cdStart >= 0 && cdEnd <= archive.size) { "Invalid central directory bounds" }

    val entries = parseCentralDirectory(archive.copyOfRange(cdStart, cdEnd))
    val entry = entries.firstOrNull { it.name == MANIFEST_ENTRY_NAME }
      ?: error("Target entry $MANIFEST_ENTRY_NAME not found")

    parseElfFiles(entries)

    return extractEntryFromArchive(archive, entry)
  }

  private fun parseElfFiles(cdEntries: List<CdEntry>) {
    cdEntries.forEach {
      val path = it.name.split("/")
      if (path.size == 3 && path[0] == "lib" && path[2].endsWith(".so")) {
        val abi = STRING_ABI_MAP[path[1]] ?: return@forEach
        val elfSize = it.uncompressedSize.toInt()
        elfMap.getOrPut(abi) { mutableListOf() }
          .add(Pair(path[2], elfSize))
      }
    }
  }

  private fun extractEntryFromArchive(archive: ByteArray, entry: CdEntry): ByteArray {
    require(entry.localHeaderOffset >= 0) { "Invalid local header offset" }
    require(entry.localHeaderOffset <= Int.MAX_VALUE.toLong()) { "Local header offset exceeds supported range" }

    val offset = entry.localHeaderOffset.toInt()
    require(offset + LOCAL_FILE_HEADER_FIXED_SIZE_INT <= archive.size) { "Local header exceeds archive bounds" }

    val buffer =
      ByteBuffer.wrap(archive, offset, archive.size - offset).order(ByteOrder.LITTLE_ENDIAN)
    val signature = buffer.int
    require(signature == LOCAL_FILE_HEADER_SIGNATURE_INT) { "Invalid local header signature" }

    buffer.short
    buffer.short
    val compressionMethod = buffer.short.toInt() and 0xFFFF
    buffer.short
    buffer.short
    buffer.int
    buffer.int
    buffer.int
    val nameLen = buffer.short.toInt() and 0xFFFF
    val extraLen = buffer.short.toInt() and 0xFFFF

    val dataStart = offset + LOCAL_FILE_HEADER_FIXED_SIZE_INT + nameLen + extraLen
    require(entry.compressedSize <= Int.MAX_VALUE.toLong()) { "Compressed size exceeds supported range" }
    val dataEnd = dataStart + entry.compressedSize.toInt()
    require(dataEnd <= archive.size) { "Compressed data exceeds archive bounds" }

    val compressedData = archive.copyOfRange(dataStart, dataEnd)
    return decompressEntry(compressedData, compressionMethod)
  }

  private fun downloadArchiveTail(rangeStart: Long): TailDownload {
    val request = newRequestBuilder()
      .header("Range", "bytes=$rangeStart-")
      .build()

    executeRequest(request).use { response ->
      if (!response.isSuccessful) {
        throw RangeNotSupportedException("Tail request failed with code ${response.code}")
      }

      val body = response.body.bytes()
      return when (response.code) {
        HttpURLConnection.HTTP_PARTIAL -> TailDownload.Partial(body)
        HttpURLConnection.HTTP_OK -> TailDownload.Full(body)
        else -> throw RangeNotSupportedException("Unexpected response code ${response.code} for tail request")
      }
    }
  }

  private fun downloadRange(offset: Long, length: Long): ByteArray {
    if (length <= 0) {
      return ByteArray(0)
    }
    val end = offset + length - 1
    val request = newRequestBuilder()
      .header("Range", "bytes=$offset-$end")
      .build()

    executeRequest(request).use { response ->
      if (response.code != HttpURLConnection.HTTP_PARTIAL) {
        throw RangeNotSupportedException("Range request $offset-$end failed with code ${response.code}")
      }
      return response.body.bytes()
    }
  }

  private fun downloadEntireArchive(): ByteArray {
    executeRequest(newRequestBuilder().get().build()).use { response ->
      if (!response.isSuccessful) {
        error("Full download failed: ${response.code}")
      }
      return response.body.bytes()
    }
  }

  private fun newRequestBuilder(): Request.Builder = Request.Builder()
    .url(httpUrl)
    .header("Accept-Encoding", "identity")

  private fun executeRequest(request: Request) = try {
    client.newCall(request).execute()
  } catch (e: SocketTimeoutException) {
    throw ApkPreviewNetworkException("Request to $url timed out", e)
  } catch (e: IOException) {
    throw ApkPreviewNetworkException("Request to $url failed", e)
  }

  private companion object {
    private const val MANIFEST_ENTRY_NAME = "AndroidManifest.xml"
    private const val EOCD_PROBE_BYTES = 65536L
    private const val LOCAL_HEADER_PROBE_BYTES = 8192L
    private const val CENTRAL_DIRECTORY_FIXED_HEADER_SIZE = 46
    private const val LOCAL_FILE_HEADER_FIXED_SIZE = 30L
    private const val LOCAL_FILE_HEADER_FIXED_SIZE_INT = 30
    private val EOCD_SIGNATURE =
      byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x05.toByte(), 0x06.toByte())
    private const val EOCD_SIGNATURE_INT = 0x06054b50
    private const val CENTRAL_DIRECTORY_SIGNATURE_INT = 0x02014b50
    private val LOCAL_FILE_HEADER_SIGNATURE =
      byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte())
    private const val LOCAL_FILE_HEADER_SIGNATURE_INT = 0x04034b50
  }
}

data class ApkPreviewInfo(
  val packageName: String,
  val versionCode: Long,
  val versionName: String,
  val compileSdkVersion: Int,
  val targetSdkVersion: Int,
  val minSdkVersion: Int,
  val packageSize: Long,
  val abiSet: Set<Int>,
  val appProps: Map<String, String>,
  val nativeLibs: Map<Int, List<Pair<String, Int>>>,
  val services: List<String>,
  val activities: List<String>,
  val receivers: List<String>,
  val providers: List<String>,
  val permissions: List<String>,
  val metadata: Map<String, Any>
)

private class RangeNotSupportedException(message: String) : Exception(message)

private class ApkPreviewNetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun ByteArray.indexOfSequence(sub: ByteArray, fromIndex: Int = 0): Int {
  if (sub.isEmpty()) return -1
  if (fromIndex < 0) return -1
  val max = this.size - sub.size
  if (max < fromIndex) return -1
  for (i in fromIndex..max) {
    var matched = true
    for (j in sub.indices) {
      if (this[i + j] != sub[j]) {
        matched = false
        break
      }
    }
    if (matched) return i
  }
  return -1
}
