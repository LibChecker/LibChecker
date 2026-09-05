package com.absinthe.libchecker.utils.dex

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Arrays

/**
 * Finds selected class descriptors or exact strings without materializing the complete DEX in the Java heap.
 *
 * DEX identifier tables and class definitions precede their string data. Keeping only those
 * integer tables lets us stream a compressed DEX entry and retain bounded working memory even
 * when an APK contains an unusually large DEX.
 */
internal object StreamingDexClassScanner {

  /** Reads only the header; class definitions do not need to be decoded to count them. */
  fun countClasses(inputStream: InputStream, entrySize: Long): Int {
    val cursor = InputCursor(inputStream)
    val baseHeader = cursor.readBytes(DEX_HEADER_SIZE)
    val version = baseHeader.readVersion()
    val headerSize = if (version == DEX_CONTAINER_VERSION) DEX_CONTAINER_HEADER_SIZE else DEX_HEADER_SIZE
    val header = if (headerSize > baseHeader.size) {
      baseHeader + cursor.readBytes(headerSize - baseHeader.size)
    } else {
      baseHeader
    }
    validateHeader(header, version, headerSize)
    val dataLimit = resolveDataLimit(header, version, headerSize, header.readIntLe(FILE_SIZE_OFFSET), entrySize)
    val count = header.readIntLe(CLASS_DEFS_SIZE_OFFSET)
    val typeCount = header.readIntLe(TYPE_IDS_SIZE_OFFSET)
    validateTable(typeCount, header.readIntLe(TYPE_IDS_OFFSET_OFFSET), UINT_SIZE, headerSize, dataLimit, MAX_TYPE_IDS)
    validateTable(count, header.readIntLe(CLASS_DEFS_OFFSET_OFFSET), CLASS_DEF_ITEM_SIZE, headerSize, dataLimit, MAX_TYPE_IDS)
    if (count > typeCount) throw IOException("DEX class count exceeds its type count")
    return count
  }

  fun findClasses(
    inputStream: InputStream,
    classPatterns: List<String>,
    hasAny: Boolean = false,
    entrySize: Long? = null
  ): List<String> {
    if (classPatterns.isEmpty()) return emptyList()

    val patterns = classPatterns.distinct()
    val found = BooleanArray(patterns.size)
    val cursor = InputCursor(inputStream)
    val baseHeader = cursor.readBytes(DEX_HEADER_SIZE)
    val version = baseHeader.readVersion()
    val headerSize = if (version == DEX_CONTAINER_VERSION) {
      DEX_CONTAINER_HEADER_SIZE
    } else {
      DEX_HEADER_SIZE
    }
    val header = if (headerSize > baseHeader.size) {
      baseHeader + cursor.readBytes(headerSize - baseHeader.size)
    } else {
      baseHeader
    }
    validateHeader(header, version, headerSize)

    val fileSize = header.readIntLe(FILE_SIZE_OFFSET)
    val stringIdsSize = header.readIntLe(STRING_IDS_SIZE_OFFSET)
    val stringIdsOffset = header.readIntLe(STRING_IDS_OFFSET_OFFSET)
    val typeIdsSize = header.readIntLe(TYPE_IDS_SIZE_OFFSET)
    val typeIdsOffset = header.readIntLe(TYPE_IDS_OFFSET_OFFSET)
    val classDefsSize = header.readIntLe(CLASS_DEFS_SIZE_OFFSET)
    val classDefsOffset = header.readIntLe(CLASS_DEFS_OFFSET_OFFSET)
    val dataLimit = resolveDataLimit(header, version, headerSize, fileSize, entrySize)

    if (classDefsSize == 0) return emptyList()

    validateTable(stringIdsSize, stringIdsOffset, UINT_SIZE, headerSize, dataLimit, MAX_STRING_IDS)
    validateTable(typeIdsSize, typeIdsOffset, UINT_SIZE, headerSize, dataLimit, MAX_TYPE_IDS)
    validateTable(classDefsSize, classDefsOffset, CLASS_DEF_ITEM_SIZE, headerSize, dataLimit, MAX_TYPE_IDS)
    if (
      classDefsSize > typeIdsSize ||
      stringIdsOffset > typeIdsOffset ||
      typeIdsOffset > classDefsOffset
    ) {
      throw IOException("DEX identifier tables are invalid")
    }
    val allocationBytes = (
      stringIdsSize.toLong() +
        typeIdsSize.toLong() +
        classDefsSize.toLong()
      ) * UINT_SIZE
    if (allocationBytes > MAX_INDEX_BYTES) {
      throw IOException("DEX identifier tables exceed the memory budget")
    }

    cursor.skipTo(stringIdsOffset)
    val stringDataOffsets = IntArray(stringIdsSize) { cursor.readIntLe() }

    cursor.skipTo(typeIdsOffset)
    val typeDescriptorIndexes = IntArray(typeIdsSize) { cursor.readIntLe() }

    cursor.skipTo(classDefsOffset)
    val classDescriptorOffsets = IntArray(classDefsSize)
    repeat(classDefsSize) { classIndex ->
      val typeIndex = cursor.readIntLe()
      if (typeIndex !in typeDescriptorIndexes.indices) {
        throw IOException("DEX class type index is out of bounds")
      }
      val descriptorIndex = typeDescriptorIndexes[typeIndex]
      if (descriptorIndex !in stringDataOffsets.indices) {
        throw IOException("DEX class descriptor index is out of bounds")
      }
      classDescriptorOffsets[classIndex] = stringDataOffsets[descriptorIndex]
      cursor.skip(CLASS_DEF_ITEM_SIZE - UINT_SIZE)
    }

    Arrays.sort(classDescriptorOffsets)
    var previousOffset = -1
    for (descriptorOffset in classDescriptorOffsets) {
      if (descriptorOffset == previousOffset) continue
      previousOffset = descriptorOffset
      if (descriptorOffset < cursor.position || descriptorOffset >= dataLimit) {
        throw IOException("DEX class descriptor offset is invalid")
      }
      cursor.skipTo(descriptorOffset)
      cursor.readUleb128()
      val descriptor = cursor.readNullTerminatedString()

      patterns.forEachIndexed { index, pattern ->
        if (!found[index] && descriptor.matchesClassPattern(pattern)) {
          found[index] = true
        }
      }
      if ((hasAny && found.any { it }) || found.all { it }) {
        break
      }
    }

    return patterns.filterIndexed { index, _ -> found[index] }
  }

  /** Finds exact string constants while retaining only the DEX string offset table in memory. */
  fun findStrings(
    inputStream: InputStream,
    stringPatterns: List<String>,
    stopAfterMatches: Int = stringPatterns.size,
    entrySize: Long? = null
  ): List<String> = findStringMatches(
    inputStream = inputStream,
    stringPatterns = stringPatterns,
    stopAfterMatches = stopAfterMatches,
    entrySize = entrySize
  ).matches

  fun findStringMatches(
    inputStream: InputStream,
    stringPatterns: List<String>,
    stopAfterMatches: Int = stringPatterns.size,
    entrySize: Long? = null,
    maxScanBytes: Int = Int.MAX_VALUE
  ): StringMatchResult {
    if (stringPatterns.isEmpty()) return StringMatchResult(emptyList(), 0)

    val patterns = stringPatterns.distinct()
    val requiredMatches = stopAfterMatches.coerceIn(1, patterns.size)
    val found = BooleanArray(patterns.size)
    val cursor = InputCursor(inputStream)
    val baseHeader = cursor.readBytes(DEX_HEADER_SIZE)
    val version = baseHeader.readVersion()
    val headerSize = if (version == DEX_CONTAINER_VERSION) {
      DEX_CONTAINER_HEADER_SIZE
    } else {
      DEX_HEADER_SIZE
    }
    val header = if (headerSize > baseHeader.size) {
      baseHeader + cursor.readBytes(headerSize - baseHeader.size)
    } else {
      baseHeader
    }
    validateHeader(header, version, headerSize)

    val fileSize = header.readIntLe(FILE_SIZE_OFFSET)
    val stringIdsSize = header.readIntLe(STRING_IDS_SIZE_OFFSET)
    val stringIdsOffset = header.readIntLe(STRING_IDS_OFFSET_OFFSET)
    val dataLimit = resolveDataLimit(header, version, headerSize, fileSize, entrySize)
    if (stringIdsSize == 0) return StringMatchResult(emptyList(), cursor.position)

    validateTable(stringIdsSize, stringIdsOffset, UINT_SIZE, headerSize, dataLimit, MAX_STRING_IDS)
    if (stringIdsSize.toLong() * UINT_SIZE > MAX_INDEX_BYTES) {
      throw IOException("DEX string identifiers exceed the memory budget")
    }
    cursor.setReadLimit(minOf(dataLimit, maxScanBytes))

    cursor.skipTo(stringIdsOffset)
    val stringDataOffsets = IntArray(stringIdsSize) { cursor.readIntLe() }
    Arrays.sort(stringDataOffsets)
    val patternBytes = patterns.map { it.toByteArray(StandardCharsets.UTF_8) }
    val candidates = BooleanArray(patterns.size)
    var matchCount = 0
    var previousOffset = -1
    for (stringOffset in stringDataOffsets) {
      if (stringOffset == previousOffset) continue
      previousOffset = stringOffset
      if (stringOffset < cursor.position || stringOffset >= dataLimit) {
        throw IOException("DEX string data offset is invalid")
      }
      cursor.skipTo(stringOffset)
      cursor.readUleb128()
      matchCount += cursor.matchExactStrings(patternBytes, found, candidates)
      if (matchCount >= requiredMatches) break
    }

    return StringMatchResult(patterns.filterIndexed { index, _ -> found[index] }, cursor.position)
  }

  private fun validateHeader(header: ByteArray, version: Int, expectedHeaderSize: Int) {
    if (
      header.size != expectedHeaderSize ||
      header[0] != 'd'.code.toByte() ||
      header[1] != 'e'.code.toByte() ||
      header[2] != 'x'.code.toByte() ||
      header[3] != '\n'.code.toByte() ||
      header[7] != 0.toByte() ||
      version !in SUPPORTED_DEX_VERSIONS
    ) {
      throw IOException("Input is not a supported DEX file")
    }
    if (header.readIntLe(HEADER_SIZE_OFFSET) != expectedHeaderSize) {
      throw IOException("Unsupported DEX header size")
    }
    if (header.readIntLe(ENDIAN_TAG_OFFSET) != ENDIAN_CONSTANT) {
      throw IOException("Unsupported DEX byte order")
    }
  }

  private fun resolveDataLimit(
    header: ByteArray,
    version: Int,
    headerSize: Int,
    fileSize: Int,
    entrySize: Long?
  ): Int {
    if (fileSize < headerSize) {
      throw IOException("DEX file size is invalid")
    }

    val dataLimit = if (version == DEX_CONTAINER_VERSION) {
      val containerSize = header.readIntLe(CONTAINER_SIZE_OFFSET)
      val headerOffset = header.readIntLe(HEADER_OFFSET_OFFSET)
      if (headerOffset != 0 || containerSize < headerSize || fileSize > containerSize) {
        throw IOException("DEX container bounds are invalid")
      }
      containerSize
    } else {
      fileSize
    }
    if (entrySize != null && (entrySize < 0 || dataLimit.toLong() > entrySize)) {
      throw IOException("DEX size exceeds its archive entry")
    }
    return dataLimit
  }

  private fun validateTable(
    size: Int,
    offset: Int,
    itemSize: Int,
    headerSize: Int,
    dataLimit: Int,
    maxItems: Int
  ) {
    if (size == 0 && offset == 0) return
    val tableEnd = offset.toLong() + size.toLong() * itemSize
    if (
      size == 0 ||
      size < 0 ||
      size > maxItems ||
      offset < headerSize ||
      offset % UINT_SIZE != 0 ||
      tableEnd > dataLimit
    ) {
      throw IOException("DEX table bounds are invalid")
    }
  }

  private fun ByteArray.readVersion(): Int {
    if (
      this[0] != 'd'.code.toByte() ||
      this[1] != 'e'.code.toByte() ||
      this[2] != 'x'.code.toByte() ||
      this[3] != '\n'.code.toByte() ||
      this[4].toInt() !in '0'.code..'9'.code ||
      this[5].toInt() !in '0'.code..'9'.code ||
      this[6].toInt() !in '0'.code..'9'.code ||
      this[7] != 0.toByte()
    ) {
      throw IOException("Input is not a DEX file")
    }
    return (this[4] - '0'.code.toByte()) * 100 +
      (this[5] - '0'.code.toByte()) * 10 +
      (this[6] - '0'.code.toByte())
  }

  private fun ByteArray.readIntLe(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or
      ((this[offset + 1].toInt() and 0xff) shl 8) or
      ((this[offset + 2].toInt() and 0xff) shl 16) or
      ((this[offset + 3].toInt() and 0xff) shl 24)
  }

  private fun String.matchesClassPattern(pattern: String): Boolean {
    return if (pattern.endsWith("*")) {
      startsWith(pattern.dropLast(1))
    } else {
      this == pattern
    }
  }

  data class StringMatchResult(
    val matches: List<String>,
    val bytesRead: Int
  )

  private class InputCursor(private val inputStream: InputStream) {
    var position: Int = 0
      private set
    private var readLimit: Int = Int.MAX_VALUE
    private val readBuffer = ByteArray(INPUT_BUFFER_SIZE)
    private var readBufferOffset = 0
    private var readBufferSize = 0

    fun setReadLimit(limit: Int) {
      if (limit < position) throw IOException("DEX scan budget is smaller than its header")
      readLimit = limit
    }

    fun readBytes(count: Int): ByteArray {
      requireWithinReadLimit(count)
      val result = ByteArray(count)
      var offset = 0
      while (offset < result.size) {
        val bufferedBytes = (readBufferSize - readBufferOffset).coerceAtMost(result.size - offset)
        if (bufferedBytes > 0) {
          readBuffer.copyInto(result, offset, readBufferOffset, readBufferOffset + bufferedBytes)
          readBufferOffset += bufferedBytes
          offset += bufferedBytes
          position += bufferedBytes
          continue
        }
        val read = inputStream.read(result, offset, result.size - offset)
        if (read < 0) throw IOException("Unexpected end of DEX")
        if (read == 0) continue
        offset += read
        position += read
      }
      return result
    }

    fun readIntLe(): Int {
      val b0 = readByte()
      val b1 = readByte()
      val b2 = readByte()
      val b3 = readByte()
      return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun readUleb128(): Int {
      var result = 0
      for (index in 0 until MAX_ULEB128_BYTES) {
        val current = readByte()
        result = result or ((current and 0x7f) shl (index * 7))
        if ((current and 0x80) == 0) return result
      }
      throw IOException("Invalid DEX ULEB128 value")
    }

    fun readNullTerminatedString(): String {
      val output = ByteArrayOutputStream()
      while (output.size() <= MAX_DESCRIPTOR_BYTES) {
        val current = readByte()
        if (current == 0) {
          return output.toString(StandardCharsets.UTF_8.name())
        }
        output.write(current)
      }
      throw IOException("DEX string is too long")
    }

    fun skipTo(targetPosition: Int) {
      if (targetPosition < position) {
        throw IOException("DEX offsets are not streamable")
      }
      skip(targetPosition - position)
    }

    fun skip(count: Int) {
      requireWithinReadLimit(count)
      var remaining = count
      val bufferedBytes = (readBufferSize - readBufferOffset).coerceAtMost(remaining)
      if (bufferedBytes > 0) {
        readBufferOffset += bufferedBytes
        remaining -= bufferedBytes
        position += bufferedBytes
      }
      while (remaining > 0) {
        val skipped = inputStream.skip(remaining.toLong()).coerceAtMost(remaining.toLong()).toInt()
        if (skipped > 0) {
          remaining -= skipped
          position += skipped
        } else {
          readByte()
          remaining--
        }
      }
    }

    private fun readByte(): Int {
      requireWithinReadLimit(1)
      if (readBufferOffset == readBufferSize) {
        do {
          readBufferSize = inputStream.read(readBuffer)
        } while (readBufferSize == 0)
        if (readBufferSize < 0) throw IOException("Unexpected end of DEX")
        readBufferOffset = 0
      }
      val value = readBuffer[readBufferOffset++].toInt() and 0xff
      position++
      return value
    }

    fun matchExactStrings(
      patterns: List<ByteArray>,
      found: BooleanArray,
      candidates: BooleanArray
    ): Int {
      var activeCandidates = 0
      patterns.indices.forEach { index ->
        candidates[index] = !found[index]
        if (candidates[index]) activeCandidates++
      }
      var stringLength = 0
      while (true) {
        val current = readByte()
        if (current == 0) break
        if (activeCandidates > 0) {
          patterns.indices.forEach { index ->
            if (
              candidates[index] &&
              (stringLength >= patterns[index].size || current != (patterns[index][stringLength].toInt() and 0xff))
            ) {
              candidates[index] = false
              activeCandidates--
            }
          }
        }
        stringLength++
      }

      var newMatches = 0
      patterns.indices.forEach { index ->
        if (candidates[index] && stringLength == patterns[index].size) {
          found[index] = true
          newMatches++
        }
      }
      return newMatches
    }

    private fun requireWithinReadLimit(count: Int) {
      if (count < 0 || position.toLong() + count > readLimit) {
        throw IOException("DEX scan exceeds its read budget")
      }
    }
  }

  private const val DEX_HEADER_SIZE = 0x70
  private const val DEX_CONTAINER_HEADER_SIZE = 0x78
  private const val DEX_CONTAINER_VERSION = 41
  private const val FILE_SIZE_OFFSET = 0x20
  private const val HEADER_SIZE_OFFSET = 0x24
  private const val ENDIAN_TAG_OFFSET = 0x28
  private const val STRING_IDS_SIZE_OFFSET = 0x38
  private const val STRING_IDS_OFFSET_OFFSET = 0x3c
  private const val TYPE_IDS_SIZE_OFFSET = 0x40
  private const val TYPE_IDS_OFFSET_OFFSET = 0x44
  private const val CLASS_DEFS_SIZE_OFFSET = 0x60
  private const val CLASS_DEFS_OFFSET_OFFSET = 0x64
  private const val CONTAINER_SIZE_OFFSET = 0x70
  private const val HEADER_OFFSET_OFFSET = 0x74
  private const val ENDIAN_CONSTANT = 0x12345678
  private const val UINT_SIZE = 4
  private const val CLASS_DEF_ITEM_SIZE = 32
  private const val MAX_STRING_IDS = 8_000_000
  private const val MAX_TYPE_IDS = 65_535
  private const val MAX_INDEX_BYTES = 36L * 1024 * 1024
  private const val MAX_ULEB128_BYTES = 5
  private const val MAX_DESCRIPTOR_BYTES = 1024 * 1024
  private const val INPUT_BUFFER_SIZE = 8 * 1024
  private val SUPPORTED_DEX_VERSIONS = setOf(35, 37, 38, 39, 40, 41)
}
