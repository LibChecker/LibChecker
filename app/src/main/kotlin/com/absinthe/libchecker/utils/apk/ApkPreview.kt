package com.absinthe.libchecker.utils.apk

import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.utils.extensions.STRING_ABI_MAP
import com.absinthe.libchecker.utils.manifest.FullManifestReader
import com.absinthe.libraries.utils.manager.TimeRecorder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber

class ApkPreview internal constructor(
  val url: String,
  private val client: OkHttpClient,
  private val cacheDir: File
) {
  constructor(url: String) : this(url, ApiManager.okHttpClient, LibCheckerApp.app.cacheDir)

  private val httpUrl = url.toHttpUrlOrNull()
  private val elfMap: MutableMap<Int, MutableList<Pair<String, Int>>> = mutableMapOf()
  private val activeCall = AtomicReference<Call?>()
  private val readMutex = Mutex()
  private var rangeArchiveLength: Long? = null
  private var checkCancelled: () -> Unit = {}

  private suspend fun <T> cancellableRead(block: () -> T): T = readMutex.withLock {
    withContext(Dispatchers.IO) {
      readCancellableArchive(block)
    }
  }

  private suspend fun <T> readCancellableArchive(block: () -> T): T = coroutineScope {
    val context = currentCoroutineContext()
    checkCancelled = { context.ensureActive() }
    // Keep cancellation connected after headers arrive, including a blocked body read.
    val cancellation = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
      try {
        awaitCancellation()
      } finally {
        activeCall.get()?.cancel()
      }
    }
    try {
      checkCancelled()
      block()
    } catch (e: Exception) {
      context.ensureActive()
      throw e
    } finally {
      cancellation.cancel()
    }
  }

  suspend fun parse(): Result<ApkPreviewInfo> = try {
    Result.success(cancellableRead { parseArchive() })
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Result.failure(e)
  }

  internal suspend fun readManifestBytes(): ByteArray = cancellableRead {
    fetchManifest(fetchMetadata())
  }

  private fun parseArchive(): ApkPreviewInfo {
    if (httpUrl == null) {
      throw IllegalArgumentException("Invalid URL: $url")
    }
    val recorder = TimeRecorder().apply { start() }
    val metadata = fetchMetadata()

    val manifestBytes = fetchManifest(metadata)
    checkCancelled()

    @Suppress("UNCHECKED_CAST")
    val manifestReader = FullManifestReader(manifestBytes, null)
    val manifestProperties = manifestReader.properties as Map<String, Any?>

    recorder.end()
    Timber.d("Parsed manifest preview from %s in %s", url, recorder)
    Timber.d("Parsed manifest preview  %s", manifestProperties["minSdkVersion"])

    return ApkPreviewInfo(
      packageName = (manifestProperties["package"] as? String).orEmpty(),
      versionCode = (manifestProperties["versionCode"] as? String)?.toLong() ?: -1L,
      versionName = (manifestProperties["versionName"] as? String).orEmpty(),
      compileSdkVersion = (manifestProperties["compileSdkVersion"] as? String)?.toInt() ?: -1,
      targetSdkVersion = (manifestProperties["targetSdkVersion"] as? String)?.toInt() ?: -1,
      minSdkVersion = (manifestProperties["minSdkVersion"] as? String)?.toInt() ?: -1,
      packageSize = metadata.contentLength,
      abiSet = elfMap.keys.toSet(),
      appProps = manifestReader.properties.map { it -> it.key to (it.value?.toString() ?: "") }.toMap(),
      nativeLibs = elfMap.mapValues { it.value.toList() },
      services = manifestReader.services,
      activities = manifestReader.activities,
      receivers = manifestReader.receivers,
      providers = manifestReader.providers,
      permissions = manifestReader.permissionList,
      metadata = manifestReader.metadata
    )
  }

  private fun fetchManifest(metadata: FileMetadata): ByteArray {
    elfMap.clear()
    rangeArchiveLength = metadata.contentLength.takeIf { it > 0 }
    return try {
      if (metadata.supportsRange && metadata.contentLength > 0) {
        fetchManifestWithRanges(metadata.contentLength)
      } else {
        fetchManifestWithFullArchive()
      }
    } catch (e: RangeNotSupportedException) {
      Timber.w(e, "Falling back to full download for %s", url)
      elfMap.clear()
      fetchManifestWithFullArchive()
    }
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

  private fun findEocdOffset(bytes: ByteArray): Int? {
    for (i in bytes.size - EOCD_MIN_SIZE downTo 0) {
      if (!bytes.matchesAt(i, EOCD_SIGNATURE)) continue
      val commentLength = ByteBuffer.wrap(bytes, i + 20, 2)
        .order(ByteOrder.LITTLE_ENDIAN)
        .short.toInt() and 0xFFFF
      if (i + EOCD_MIN_SIZE + commentLength == bytes.size) {
        Timber.d("EOCD signature found at offset %d", i)
        return i
      }
    }
    Timber.w("EOCD signature not found in provided bytes")
    return null
  }

  private fun resolveDirectoryInfo(
    bytes: ByteArray,
    absoluteStart: Long,
    fetchRecord: (Long) -> ByteArray
  ): EocdInfo {
    val eocdOffset = findEocdOffset(bytes) ?: error("EOCD signature not found")
    val eocd = parseEocd(bytes.copyOfRange(eocdOffset, bytes.size))
    val needsZip64 = eocd.totalEntries == ZIP32_MAX_ENTRIES ||
      eocd.centralDirectorySize == ZIP32_MAX_VALUE ||
      eocd.centralDirectoryOffset == ZIP32_MAX_VALUE
    if (!needsZip64) return eocd

    val locatorOffset = eocdOffset - ZIP64_LOCATOR_SIZE
    require(locatorOffset >= 0 && bytes.matchesAt(locatorOffset, ZIP64_LOCATOR_SIGNATURE)) {
      "ZIP64 EOCD locator is missing"
    }
    val locator = ByteBuffer.wrap(bytes, locatorOffset, ZIP64_LOCATOR_SIZE).order(ByteOrder.LITTLE_ENDIAN)
    locator.int
    locator.int
    val recordOffset = locator.long
    locator.int
    require(recordOffset >= 0) { "Invalid ZIP64 EOCD offset" }

    val relativeRecordOffset = recordOffset - absoluteStart
    val recordBytes = if (
      relativeRecordOffset >= 0 &&
      relativeRecordOffset + ZIP64_EOCD_MIN_SIZE <= bytes.size
    ) {
      bytes.copyOfRange(
        relativeRecordOffset.toInt(),
        relativeRecordOffset.toInt() + ZIP64_EOCD_MIN_SIZE
      )
    } else {
      fetchRecord(recordOffset)
    }
    return parseZip64Eocd(recordBytes)
  }

  private fun parseZip64Eocd(bytes: ByteArray): EocdInfo {
    require(bytes.size >= ZIP64_EOCD_MIN_SIZE) { "ZIP64 EOCD is truncated" }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    require(buffer.int == ZIP64_EOCD_SIGNATURE_INT) { "Invalid ZIP64 EOCD signature" }
    buffer.long
    buffer.short
    buffer.short
    buffer.int
    buffer.int
    buffer.long
    val totalEntries = buffer.long
    val cdSize = buffer.long
    val cdOffset = buffer.long
    require(totalEntries in 0..Int.MAX_VALUE.toLong()) { "ZIP64 entry count is too large" }
    require(cdSize >= 0 && cdOffset >= 0) { "Invalid ZIP64 central directory bounds" }
    return EocdInfo(cdSize, cdOffset, totalEntries.toInt(), "")
  }

  private data class FileMetadata(val contentLength: Long, val supportsRange: Boolean)

  private sealed class TailDownload {
    data class Partial(val bytes: ByteArray) : TailDownload()
    data class Full(val manifest: ByteArray) : TailDownload()
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
      if (index % 1024 == 0) checkCancelled()
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

      checkCancelled()
      require(entries.size < MAX_DIRECTORY_ENTRIES) { "Too many ZIP entries for preview" }
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

    validateManifest(entry)
    val localHeader = readLocalHeader(entry.localHeaderOffset)
    val dataStart =
      entry.localHeaderOffset + LOCAL_FILE_HEADER_FIXED_SIZE + localHeader.nameLength + localHeader.extraLength
    val compressedData = downloadRange(dataStart, entry.compressedSize)

    val result = decompressEntry(compressedData, localHeader.compressionMethod)
    Timber.d("Decompressed size for %s: %d bytes", entry.name, result.size)
    return result
  }

  private fun decompressEntry(compressedData: ByteArray, method: Int): ByteArray = when (method) {
    0 -> compressedData.also { require(it.size <= MAX_MANIFEST_BYTES) { "Manifest exceeds preview budget" } }

    8 -> if (compressedData.isEmpty()) {
      ByteArray(0)
    } else {
      val inflater = Inflater(true)
      try {
        InflaterInputStream(compressedData.inputStream(), inflater).use {
          readBounded(it, MAX_MANIFEST_BYTES)
        }
      } finally {
        inflater.end()
      }
    }

    else -> error("Unsupported compression method: $method")
  }

  private fun fetchMetadata(): FileMetadata {
    if (httpUrl == null) {
      throw IllegalArgumentException("Invalid URL: $url")
    }
    val isAwsPresignedUrl = httpUrl.queryParameterNames.any { it.startsWith("X-Amz-") }

    if (!isAwsPresignedUrl) {
      val headRequest = newRequestBuilder().head().build()
      val headResult = try {
        executeRequest(headRequest)
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        null
      }

      headResult?.use { response ->
        if (response.isSuccessful) {
          val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
          val supportsRange =
            response.header("Accept-Ranges")?.contains("bytes", ignoreCase = true) == true
          return FileMetadata(contentLength, supportsRange)
        }
        if (response.code != HttpURLConnection.HTTP_BAD_REQUEST &&
          response.code != HttpURLConnection.HTTP_UNAUTHORIZED &&
          response.code != HttpURLConnection.HTTP_FORBIDDEN &&
          response.code != HttpURLConnection.HTTP_BAD_METHOD &&
          response.code != HttpURLConnection.HTTP_NOT_IMPLEMENTED
        ) {
          error("Failed to fetch metadata for $url with HEAD: ${response.code}")
        }
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
    return when (val tail = downloadArchiveTail(rangeStart, contentLength)) {
      is TailDownload.Partial -> {
        val eocd = runCatching {
          resolveDirectoryInfo(tail.bytes, rangeStart) { offset ->
            downloadRange(offset, ZIP64_EOCD_MIN_SIZE.toLong())
          }
        }.getOrElse {
          if (it is CancellationException) throw it
          throw RangeNotSupportedException(it.message ?: "Unable to parse ZIP directory")
        }
        validateDirectory(eocd, contentLength)
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

      is TailDownload.Full -> tail.manifest
    }
  }

  private fun fetchManifestWithFullArchive(): ByteArray {
    executeRequest(newRequestBuilder().get().build()).use { response ->
      check(response.code == HttpURLConnection.HTTP_OK) { "Full download failed: ${response.code}" }
      return processFullResponse(response)
    }
  }

  private fun processFullResponse(response: Response): ByteArray {
    val archive = File.createTempFile("apk-preview-", ".zip", cacheDir)
    try {
      response.body.byteStream().use { input ->
        archive.outputStream().use { output ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
          while (true) {
            checkCancelled()
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
          }
        }
      }
      return RandomAccessFile(archive, "r").use { file ->
        val tailStart = (file.length() - EOCD_PROBE_BYTES).coerceAtLeast(0)
        val tail = readRange(file, tailStart, file.length() - tailStart)
        val eocd = resolveDirectoryInfo(tail, tailStart) { offset ->
          readRange(file, offset, ZIP64_EOCD_MIN_SIZE.toLong())
        }
        validateDirectory(eocd, file.length())
        val entries = parseCentralDirectory(readRange(file, eocd.centralDirectoryOffset, eocd.centralDirectorySize))
        val entry = entries.firstOrNull { it.name == MANIFEST_ENTRY_NAME }
          ?: error("Target entry $MANIFEST_ENTRY_NAME not found")
        parseElfFiles(entries)
        validateManifest(entry)
        val header = ByteBuffer.wrap(readRange(file, entry.localHeaderOffset, LOCAL_FILE_HEADER_FIXED_SIZE))
          .order(ByteOrder.LITTLE_ENDIAN)
        require(header.int == LOCAL_FILE_HEADER_SIGNATURE_INT) { "Invalid local header signature" }
        val method = header.getShort(8).toInt() and 0xFFFF
        val nameLength = header.getShort(26).toInt() and 0xFFFF
        val extraLength = header.getShort(28).toInt() and 0xFFFF
        val dataStart = entry.localHeaderOffset + LOCAL_FILE_HEADER_FIXED_SIZE + nameLength + extraLength
        decompressEntry(readRange(file, dataStart, entry.compressedSize), method)
      }
    } finally {
      archive.delete()
    }
  }

  private fun validateDirectory(eocd: EocdInfo, archiveLength: Long) {
    require(eocd.centralDirectorySize in 0..MAX_RANGE_BYTES.toLong()) { "Central directory exceeds preview budget" }
    require(eocd.totalEntries in 0..MAX_DIRECTORY_ENTRIES) { "Too many ZIP entries for preview" }
    require(
      eocd.centralDirectoryOffset in 0..archiveLength &&
        eocd.centralDirectorySize <= archiveLength - eocd.centralDirectoryOffset
    ) { "Invalid central directory bounds" }
  }

  private fun validateManifest(entry: CdEntry) {
    require(
      entry.compressedSize in 0..MAX_MANIFEST_BYTES.toLong() &&
        entry.uncompressedSize in 0..MAX_MANIFEST_BYTES.toLong()
    ) { "Manifest exceeds preview budget" }
  }

  private fun readRange(file: RandomAccessFile, offset: Long, length: Long): ByteArray {
    require(
      offset in 0..file.length() && length in 0..MAX_RANGE_BYTES.toLong() &&
        length <= file.length() - offset
    ) { "Invalid ZIP range" }
    checkCancelled()
    file.seek(offset)
    val bytes = ByteArray(length.toInt())
    var position = 0
    while (position < bytes.size) {
      checkCancelled()
      val count = minOf(DEFAULT_BUFFER_SIZE, bytes.size - position)
      file.readFully(bytes, position, count)
      position += count
    }
    return bytes
  }

  private fun parseElfFiles(cdEntries: List<CdEntry>) {
    cdEntries.forEach {
      checkCancelled()
      val path = it.name.split("/")
      if (path.size == 3 && path[0] == "lib" && path[2].endsWith(".so")) {
        val abi = STRING_ABI_MAP[path[1]] ?: return@forEach
        val elfSize = it.uncompressedSize.toInt()
        elfMap.getOrPut(abi) { mutableListOf() }
          .add(Pair(path[2], elfSize))
      }
    }
  }

  private fun downloadArchiveTail(rangeStart: Long, contentLength: Long): TailDownload {
    val request = newRequestBuilder()
      .header("Range", "bytes=$rangeStart-")
      .build()

    executeRequest(request).use { response ->
      if (!response.isSuccessful) {
        throw RangeNotSupportedException("Tail request failed with code ${response.code}")
      }

      return when (response.code) {
        HttpURLConnection.HTTP_PARTIAL -> {
          validateContentRange(response, rangeStart, contentLength - 1, contentLength)
          val bytes = readBounded(response.body.byteStream(), EOCD_PROBE_BYTES.toInt())
          require(bytes.size.toLong() == contentLength - rangeStart) { "Truncated tail response" }
          TailDownload.Partial(bytes)
        }

        HttpURLConnection.HTTP_OK -> TailDownload.Full(processFullResponse(response))

        else -> throw RangeNotSupportedException("Unexpected response code ${response.code} for tail request")
      }
    }
  }

  private fun downloadRange(offset: Long, length: Long): ByteArray {
    require(offset >= 0 && length in 0..MAX_RANGE_BYTES.toLong() && offset <= Long.MAX_VALUE - length) {
      "Invalid or excessive preview range"
    }
    if (length == 0L) {
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
      validateContentRange(response, offset, end, rangeArchiveLength)
      val bytes = readBounded(response.body.byteStream(), length.toInt())
      require(bytes.size.toLong() == length) { "Truncated range response" }
      return bytes
    }
  }

  private fun validateContentRange(response: Response, start: Long, end: Long, total: Long? = null) {
    val match = CONTENT_RANGE.matchEntire(response.header("Content-Range").orEmpty())
      ?: throw RangeNotSupportedException("Missing or invalid Content-Range")
    val actualStart = match.groupValues[1].toLongOrNull()
    val actualEnd = match.groupValues[2].toLongOrNull()
    val actualTotal = match.groupValues[3].toLongOrNull()
    if (actualStart != start || actualEnd == null || actualEnd < start || actualEnd != end ||
      (actualTotal != null && actualEnd >= actualTotal) ||
      (total != null && actualTotal != total)
    ) {
      throw RangeNotSupportedException("Unexpected Content-Range")
    }
  }

  private fun readBounded(input: InputStream, budget: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE, budget))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
      checkCancelled()
      val count = input.read(buffer, 0, minOf(buffer.size, budget - output.size() + 1))
      if (count < 0) break
      require(count <= budget - output.size()) { "Response exceeds preview budget" }
      output.write(buffer, 0, count)
    }
    return output.toByteArray()
  }

  private fun newRequestBuilder(): Request.Builder = Request.Builder()
    .url(httpUrl!!)
    .header("Accept-Encoding", "identity")

  private fun executeRequest(request: Request) = try {
    checkCancelled()
    val call = client.newCall(request)
    activeCall.set(call)
    checkCancelled()
    call.execute()
  } catch (e: SocketTimeoutException) {
    checkCancelled()
    throw ApkPreviewNetworkException("Request to $url timed out", e)
  } catch (e: IOException) {
    checkCancelled()
    throw ApkPreviewNetworkException("Request to $url failed", e)
  }

  private companion object {
    private const val MANIFEST_ENTRY_NAME = "AndroidManifest.xml"
    private const val EOCD_MIN_SIZE = 22
    private const val ZIP64_LOCATOR_SIZE = 20
    private const val ZIP64_EOCD_MIN_SIZE = 56
    private const val EOCD_PROBE_BYTES = 65557L + ZIP64_LOCATOR_SIZE + ZIP64_EOCD_MIN_SIZE
    private const val ZIP32_MAX_ENTRIES = 0xFFFF
    private const val ZIP32_MAX_VALUE = 0xFFFFFFFFL
    private const val LOCAL_HEADER_PROBE_BYTES = 30L

    // Bound only preview structures, never the complete APK streamed to disk.
    private const val MAX_RANGE_BYTES = 32 * 1024 * 1024
    private const val MAX_MANIFEST_BYTES = 16 * 1024 * 1024
    private const val MAX_DIRECTORY_ENTRIES = 250_000
    private val CONTENT_RANGE = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+|\\*)")
    private const val CENTRAL_DIRECTORY_FIXED_HEADER_SIZE = 46
    private const val LOCAL_FILE_HEADER_FIXED_SIZE = 30L
    private val EOCD_SIGNATURE =
      byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x05.toByte(), 0x06.toByte())
    private const val EOCD_SIGNATURE_INT = 0x06054b50
    private val ZIP64_LOCATOR_SIGNATURE = byteArrayOf(0x50, 0x4B, 0x06, 0x07)
    private const val ZIP64_EOCD_SIGNATURE_INT = 0x06064b50
    private const val CENTRAL_DIRECTORY_SIGNATURE_INT = 0x02014b50
    private val LOCAL_FILE_HEADER_SIGNATURE =
      byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte())
    private const val LOCAL_FILE_HEADER_SIGNATURE_INT = 0x04034b50
  }
}

private fun ByteArray.matchesAt(offset: Int, expected: ByteArray): Boolean {
  if (offset < 0 || offset + expected.size > size) return false
  return expected.indices.all { this[offset + it] == expected[it] }
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
