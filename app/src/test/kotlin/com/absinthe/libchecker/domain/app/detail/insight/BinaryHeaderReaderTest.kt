package com.absinthe.libchecker.domain.app.detail.insight

import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightDefinition.Check
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightDefinition.Layout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BinaryHeaderReaderTest {
  @Test
  fun resolvesCheckedOffsetsAndUnsignedFieldWidths() {
    val bytes = ByteBuffer.allocate(128)
    bytes.putInt(0, -1)
    bytes.putLong(4, 0x123456789)
    bytes.put(13, 0xfe.toByte())
    val value = "123.4.56b7"
    bytes.position(16)
    bytes.put(value.toByteArray()).put(0)
    bytes.order(ByteOrder.LITTLE_ENDIAN).putInt(0x7f80)
    val checks = listOf(
      Check("u32be@0", "eq", "4294967295"),
      Check("u64be@4", "eq", "4886718345"),
      Check("u8@13", "eq", "254"),
      Check("u32le@end", "eq", "32640"),
      Check("end+4", "le", "size"),
      Check("size", "ge", "u8@13+1")
    )
    val layout = Layout(16, checks)
    assertEquals(value, BinaryHeaderReader.read(bytes.array(), 300, listOf(layout), listOf("#.#.#[ab]#")))
    bytes.put(4, 0x80.toByte())
    assertNull(BinaryHeaderReader.read(bytes.array(), 300, listOf(layout), listOf("#.#.#[ab]#")))
  }

  @Test
  fun disallowsOverflowTruncatedCapturesAndConflictingLayouts() {
    fun checked(check: Check) = Layout(0, listOf(check))
    val bytes = "1\u00002\u0000".toByteArray()
    val valid = checked(Check("end", "eq", "2"))
    val mask = listOf("#")
    assertNull(BinaryHeaderReader.read(bytes, 4, listOf(checked(Check("size+9223372036854775807", "ge", "0"))), mask))
    assertNull(BinaryHeaderReader.read(bytes, 4, listOf(checked(Check("u32be@end", "eq", "0"))), mask))
    assertNull(BinaryHeaderReader.read(bytes, 4, listOf(valid, Layout(2, listOf(Check("end", "eq", "4")))), mask))
    assertNull(BinaryHeaderReader.read(bytes, 3, listOf(valid), mask))
    assertNull(BinaryHeaderReader.read("1".repeat(128).toByteArray(), 1000, listOf(valid), mask))
    val bounded = checked(Check("end", "le", "size"))
    val exact = ("1".repeat(128) + "\u0000").toByteArray()
    assertEquals("1".repeat(128), BinaryHeaderReader.read(exact, 129, listOf(bounded), mask))
    assertNull(BinaryHeaderReader.read(("1".repeat(129) + "\u0000").toByteArray(), 130, listOf(bounded), mask))
  }

  @Test
  fun masksMatchWholeAsciiValuesIncludingAmbiguousDigitRuns() {
    val layout = Layout(0, listOf(Check("end", "eq", "size")))
    fun read(value: String, mask: String): String? {
      val bytes = (value + "\u0000").toByteArray()
      return BinaryHeaderReader.read(bytes, bytes.size.toLong(), listOf(layout), listOf(mask))
    }
    assertEquals("1219b", read("1219b", "#1#[ab]"))
    assertNull(read("12", "#1#"))
    assertNull(read("12β", "#[ab]"))
    assertNull(read("12]", "#[ab]"))
    assertNull(read("x12", "#"))
    assertNull(read("12x", "#"))
    assertTrue(BinaryHeaderReader.validFormat("a._-9#"))
    val ascii = "word with spaces!?\u0000".toByteArray()
    assertEquals("word with spaces!?", BinaryHeaderReader.read(ascii, ascii.size.toLong(), listOf(layout), emptyList()))
    listOf("", "##", "[", "[]", "[a1]", "[a-z]", "#+", "a".repeat(81)).forEach {
      assertFalse(it, BinaryHeaderReader.validFormat(it))
    }
  }

  @Test
  fun rejectsUnsupportedExpressionsAndInvalidBounds() {
    for (term in listOf("", "-1", "9223372036854775808", "u64le@0", "u8@512", "1++2", "1+2+3", "u8@-1", "size ")) {
      assertFalse(term, BinaryHeaderReader.validLayout(Layout(0, listOf(Check(term, "eq", "0")))))
    }
    assertFalse(BinaryHeaderReader.validLayout(Layout(512, listOf(Check("0", "eq", "0")))))
    assertFalse(BinaryHeaderReader.validLayout(Layout(511, listOf(Check("0", "lt", "1")))))
    assertFalse(BinaryHeaderReader.validLayout(Layout(0, emptyList())))
    assertTrue(BinaryHeaderReader.validLayout(Layout(511, listOf(Check("u8@511", "le", "9223372036854775807")))))
  }
}
