package com.absinthe.libchecker.domain.app.detail.insight

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnityFsArchiveTest {
  @Test
  fun readsTheRequestedPrefixAndReportsTheOriginalEntrySize() {
    for (method in listOf(0, 2, 3)) {
      for (tail in listOf(false, true)) {
        val member = ByteArray(256) { it.toByte() }
        val bundle = archive(method, tail, member)
        for (limit in listOf(1, 17, 128, 512)) {
          val result = requireNotNull(read(bundle, "member.bin", limit))
          assertArrayEquals(member.copyOf(minOf(member.size, limit)), result.bytes)
          assertEquals(member.size.toLong(), result.size)
        }
        assertNull(read(bundle, "missing", 32))
        assertNull(read(bundle.copyOf(bundle.size - 1), "member.bin", 32))
        assertNull(read(bundle, "member.bin", 513))
      }
    }
  }

  private fun read(bytes: ByteArray, entry: String, limit: Int) = UnityFsArchive.readEntryPrefix(bytes.inputStream(), bytes.size.toLong(), entry, limit)

  private fun archive(method: Int, tail: Boolean, member: ByteArray): ByteArray {
    val blocks = listOf(ByteArray(12), ByteArray(7) + member.copyOf(17), member.copyOfRange(17, member.size))
    fun pack(data: ByteArray): ByteArray {
      if (method == 0) return data
      val out = ByteArrayOutputStream()
      BlockLZ4CompressorOutputStream(out).use { it.write(data) }
      return out.toByteArray()
    }
    val packed = blocks.map(::pack)
    val info = ByteArrayOutputStream()
    DataOutputStream(info).use { out ->
      out.write(ByteArray(16))
      out.writeInt(blocks.size)
      blocks.forEachIndexed { index, bytes ->
        out.writeInt(bytes.size)
        out.writeInt(packed[index].size)
        out.writeShort(method)
      }
      out.writeInt(1)
      out.writeLong(19)
      out.writeLong(member.size.toLong())
      out.writeInt(4)
      out.writeBytes("member.bin\u0000")
    }
    val directory = pack(info.toByteArray())
    val buf = ByteArrayOutputStream()
    DataOutputStream(buf).use { out ->
      out.writeBytes("UnityFS\u0000")
      out.writeInt(8)
      out.writeBytes("5.x.x\u00006000.0.1f1\u0000")
      out.writeLong(0)
      out.writeInt(directory.size)
      out.writeInt(info.size())
      out.writeInt(0x240 or method or if (tail) 0x80 else 0)
      while (buf.size() % 16 != 0) out.writeByte(0)
      if (!tail) out.write(directory)
      while (buf.size() % 16 != 0) out.writeByte(0)
      packed.forEach(out::write)
      if (tail) out.write(directory)
    }
    return buf.toByteArray().also { ByteBuffer.wrap(it).putLong(29, it.size.toLong()) }
  }
}
