package com.absinthe.libchecker.utils.apk

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import sun.misc.Unsafe

class ApkPreviewZip64Test {

  @Test
  fun `parses ZIP64 central directory metadata`() {
    val bytes = ByteBuffer.allocate(56).order(ByteOrder.LITTLE_ENDIAN).apply {
      putInt(0x06064b50)
      putLong(44)
      putShort(45)
      putShort(45)
      putInt(0)
      putInt(0)
      putLong(70_000)
      putLong(70_000)
      putLong(9_876_543_210L)
      putLong(1_234_567_890L)
    }.array()
    val method = ApkPreview::class.java.getDeclaredMethod("parseZip64Eocd", ByteArray::class.java)
      .apply { isAccessible = true }

    val result = method.invoke(newPreviewWithoutConstructor(), bytes) as ApkPreview.EocdInfo

    assertEquals(70_000, result.totalEntries)
    assertEquals(9_876_543_210L, result.centralDirectorySize)
    assertEquals(1_234_567_890L, result.centralDirectoryOffset)
  }

  @Test
  fun `finds the terminal EOCD instead of a signature inside the comment`() {
    val comment = byteArrayOf(0x50, 0x4B, 0x05, 0x06, 1, 2, 3)
    val bytes = ByteBuffer.allocate(22 + comment.size).order(ByteOrder.LITTLE_ENDIAN).apply {
      putInt(0x06054b50)
      repeat(4) { putShort(0) }
      putInt(0)
      putInt(0)
      putShort(comment.size.toShort())
      put(comment)
    }.array()
    val method = ApkPreview::class.java.getDeclaredMethod("findEocdOffset", ByteArray::class.java)
      .apply { isAccessible = true }

    assertEquals(0, method.invoke(newPreviewWithoutConstructor(), bytes))
  }

  private fun newPreviewWithoutConstructor(): ApkPreview {
    val field = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
    return (field.get(null) as Unsafe).allocateInstance(ApkPreview::class.java) as ApkPreview
  }
}
