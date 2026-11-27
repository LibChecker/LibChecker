package com.absinthe.libchecker.domain.app.detail.insight

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream

internal data class ArchiveEntryPrefix(val bytes: ByteArray, val size: Long)

internal object UnityFsArchive {
  private const val MAX_PREFIX = 512
  private const val MAX_DIRECTORY = 1024 * 1024
  private const val MAX_BLOCK = 2 * 1024 * 1024
  private const val MAX_TAIL_DATA = 4 * 1024 * 1024
  private const val MAX_BLOCKS = 32_768

  fun readEntryPrefix(input: InputStream, size: Long, entry: String, maxBytes: Int): ArchiveEntryPrefix? = try {
    if (maxBytes in 1..MAX_PREFIX) {
      readDirectoryEntry(DataInputStream(input), size, entry, maxBytes)
    } else {
      null
    }
  } catch (_: EOFException) {
    null
  } catch (_: IllegalArgumentException) {
    null
  }

  private data class Block(val size: Int, val packed: Int, val method: Int)

  private fun readDirectoryEntry(
    input: DataInputStream,
    size: Long,
    target: String,
    maxBytes: Int
  ): ArchiveEntryPrefix? {
    if (input.cString(8) != "UnityFS") return null
    val format = input.readInt()
    if (format !in 6..8) return null
    val player = input.cString(64)
    val revision = input.cString(64)
    if (input.readLong() != size) return null
    val packed = input.readInt()
    val unpacked = input.readInt()
    val flags = input.readInt()
    if (flags and 0x40 == 0 || packed !in 1..MAX_DIRECTORY || unpacked !in 1..MAX_DIRECTORY) return null
    // 34 fixed bytes include the signature and the two string terminators.
    var position = 34L + player.length + revision.length
    if (format >= 7) position += input.pad(position)
    val tail = flags and 0x80 != 0
    val dataInput: DataInputStream
    val directory: ByteArray
    val payloadSize: Long
    if (tail) {
      if (flags and 0x200 != 0) position += input.pad(position)
      payloadSize = size - position - packed
      if (payloadSize !in 1..MAX_TAIL_DATA.toLong()) return null
      dataInput = DataInputStream(input.bytes(payloadSize.toInt()).inputStream())
      directory = decode(input.bytes(packed), unpacked, flags and 63)
    } else {
      directory = decode(input.bytes(packed), unpacked, flags and 63)
      position += packed
      if (flags and 0x200 != 0) position += input.pad(position)
      payloadSize = size - position
      dataInput = input
    }
    val info = DataInputStream(directory.inputStream())
    info.discard(16)
    val count = info.readInt()
    if (count !in 1..MAX_BLOCKS) return null
    val blocks = List(count) { Block(info.readInt(), info.readInt(), info.readUnsignedShort() and 63) }
    if (blocks.any { it.size <= 0 || it.packed <= 0 } || blocks.sumOf { it.packed.toLong() } != payloadSize) return null
    val total = blocks.sumOf { it.size.toLong() }
    val entries = info.readInt()
    if (entries !in 1..MAX_BLOCKS) return null
    var offset = -1L
    var length = 0L
    repeat(entries) {
      val start = info.readLong()
      val entrySize = info.readLong()
      info.readInt()
      if (start !in 0..total || entrySize !in 0..(total - start)) return null
      if (info.cString(1024) == target) {
        if (offset >= 0) return null
        offset = start
        length = entrySize
      }
    }
    if (offset < 0) return null
    val prefix = ByteArray(minOf(maxBytes.toLong(), length).toInt())
    if (prefix.isEmpty()) return ArchiveEntryPrefix(prefix, length)
    var copied = 0
    var packedBeforeTarget = 0L
    for (block in blocks) {
      if (offset >= block.size) {
        packedBeforeTarget += block.packed
        offset -= block.size
        continue
      }
      if (packedBeforeTarget > 0) {
        dataInput.discard(packedBeforeTarget)
        packedBeforeTarget = 0
      }
      val take = minOf(block.size - offset.toInt(), prefix.size - copied)
      if (block.method == 0) {
        if (block.size != block.packed) return null
        dataInput.discard(offset)
        dataInput.readFully(prefix, copied, take)
      } else {
        if (block.size > MAX_BLOCK || block.packed > MAX_BLOCK) return null
        val bytes = decode(dataInput.bytes(block.packed), block.size, block.method)
        bytes.copyInto(prefix, copied, offset.toInt(), offset.toInt() + take)
      }
      copied += take
      if (copied == prefix.size) return ArchiveEntryPrefix(prefix, length)
      offset = 0
    }
    return null
  }

  private fun decode(bytes: ByteArray, size: Int, method: Int): ByteArray = when (method) {
    0 -> bytes.also { require(it.size == size) }

    2, 3 -> BlockLZ4CompressorInputStream(ByteArrayInputStream(bytes)).use { lz4 ->
      DataInputStream(lz4).bytes(size).also { require(lz4.read() < 0) }
    }

    else -> throw IllegalArgumentException("Unsupported block encoding")
  }

  private fun DataInputStream.cString(limit: Int): String = buildString {
    repeat(limit) {
      val value = readUnsignedByte()
      if (value == 0) return@buildString
      append(value.toChar())
    }
    throw IllegalArgumentException("Unterminated archive string")
  }

  private fun DataInputStream.bytes(size: Int): ByteArray = ByteArray(size).also(::readFully)

  private fun DataInputStream.pad(position: Long): Long = ((16 - position % 16) % 16).also { discard(it) }

  private fun DataInputStream.discard(count: Long) {
    var left = count
    val buffer = ByteArray(minOf(8192L, left).toInt())
    while (left > 0) {
      val n = minOf(left, buffer.size.toLong()).toInt()
      readFully(buffer, 0, n)
      left -= n
    }
  }
}
