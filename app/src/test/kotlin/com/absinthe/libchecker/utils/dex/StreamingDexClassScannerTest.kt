package com.absinthe.libchecker.utils.dex

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.value.ImmutableStringEncodedValue
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StreamingDexClassScannerTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun findsDefinedExactAndPrefixClassesButNotReferencedTypes() {
    val dex = createDex(
      ImmutableClassDef(
        "Lcom/example/Defined;",
        0x1,
        "Lcom/reference/Only;",
        null,
        null,
        null,
        null,
        null
      )
    )

    val found = StreamingDexClassScanner.findClasses(
      dex.inputStream(),
      listOf(
        "Lcom/example/Defined;",
        "Lcom/example/*",
        "Lcom/reference/Only;"
      ),
      entrySize = dex.size.toLong()
    )

    assertEquals(listOf("Lcom/example/Defined;", "Lcom/example/*"), found)
  }

  @Test
  fun stopsReadingAfterARequestedClassIsFound() {
    val dex = createDex(
      ImmutableClassDef(
        "Lcom/example/Defined;",
        0x1,
        "Ljava/lang/Object;",
        null,
        null,
        null,
        null,
        null
      )
    )
    val paddedDex = dex + ByteArray(8 * 1024 * 1024)
    val input = CountingInputStream(ByteArrayInputStream(paddedDex))

    val found = StreamingDexClassScanner.findClasses(
      input,
      listOf("Lcom/example/Defined;"),
      hasAny = true,
      entrySize = paddedDex.size.toLong()
    )

    assertEquals(listOf("Lcom/example/Defined;"), found)
    assertTrue(input.bytesRead < 1024 * 1024)
  }

  @Test
  fun returnsEmptyForDexWithoutDefinedClasses() {
    val dex = createDex()

    val found = StreamingDexClassScanner.findClasses(
      dex.inputStream(),
      listOf("Lcom/example/Missing;"),
      entrySize = dex.size.toLong()
    )

    assertTrue(found.isEmpty())
  }

  @Test
  fun findsExactStringEvidenceWithoutTreatingItAsDefinedClasses() {
    val dex = createDex(classWithStringConstants(KOTLIN_RUNTIME_MARKERS))

    val definedClasses = StreamingDexClassScanner.findClasses(
      dex.inputStream(),
      listOf("Lkotlin/*", "Lkotlinx/*"),
      hasAny = true,
      entrySize = dex.size.toLong()
    )
    val foundStrings = StreamingDexClassScanner.findStrings(
      dex.inputStream(),
      KOTLIN_RUNTIME_MARKERS + "kotlin.decoy.Missing",
      stopAfterMatches = KOTLIN_RUNTIME_MARKERS.size,
      entrySize = dex.size.toLong()
    )

    assertTrue(definedClasses.isEmpty())
    assertEquals(KOTLIN_RUNTIME_MARKERS, foundStrings)
  }

  @Test
  fun requiresTwoIndependentRuntimeMarkersForKotlinFallback() {
    val singleMarkerDex = createDex(classWithStringConstants(KOTLIN_RUNTIME_MARKERS.take(1)))
    val twoMarkerDex = createDex(classWithStringConstants(KOTLIN_RUNTIME_MARKERS))

    val singleMarkerMatches = StreamingDexClassScanner.findStrings(
      singleMarkerDex.inputStream(),
      KOTLIN_RUNTIME_MARKERS,
      stopAfterMatches = KOTLIN_RUNTIME_MARKERS.size,
      entrySize = singleMarkerDex.size.toLong()
    )
    val twoMarkerMatches = StreamingDexClassScanner.findStrings(
      twoMarkerDex.inputStream(),
      KOTLIN_RUNTIME_MARKERS,
      stopAfterMatches = KOTLIN_RUNTIME_MARKERS.size,
      entrySize = twoMarkerDex.size.toLong()
    )

    assertTrue(singleMarkerMatches.size < KOTLIN_RUNTIME_MARKERS.size)
    assertEquals(KOTLIN_RUNTIME_MARKERS.size, twoMarkerMatches.size)
  }

  @Test
  fun findsMarkersAfterAnOversizedNonmatchingString() {
    val dex = createDex(
      classWithStringConstants(
        listOf("a".repeat(MAX_DESCRIPTOR_BYTES + 1)) + KOTLIN_RUNTIME_MARKERS
      )
    )

    val foundStrings = StreamingDexClassScanner.findStrings(
      dex.inputStream(),
      KOTLIN_RUNTIME_MARKERS,
      stopAfterMatches = KOTLIN_RUNTIME_MARKERS.size,
      entrySize = dex.size.toLong()
    )

    assertEquals(KOTLIN_RUNTIME_MARKERS, foundStrings)
  }

  @Test
  fun findsNonAsciiExactStringEvidence() {
    val marker = "协程"
    val dex = createDex(classWithStringConstants(listOf(marker)))

    val foundStrings = StreamingDexClassScanner.findStrings(
      dex.inputStream(),
      listOf(marker),
      entrySize = dex.size.toLong()
    )

    assertEquals(listOf(marker), foundStrings)
  }

  @Test
  fun findsClassInDexVersion41ContainerHeader() {
    val dex = createDex41("Lcom/example/Container;")
    val headerInput = dex.inputStream()
    assertEquals(1, StreamingDexClassScanner.countClasses(headerInput, dex.size.toLong()))
    assertEquals(dex.size - DEX_CONTAINER_HEADER_SIZE, headerInput.available())

    val found = StreamingDexClassScanner.findClasses(
      dex.inputStream(),
      listOf("Lcom/example/Container;"),
      entrySize = dex.size.toLong()
    )

    assertEquals(listOf("Lcom/example/Container;"), found)
  }

  @Test
  fun rejectsHeaderSizeBeyondArchiveEntryBeforeReadingTables() {
    val dex = createTruncatedDex()
    val input = CountingInputStream(ByteArrayInputStream(dex))

    assertThrows(IOException::class.java) {
      StreamingDexClassScanner.findClasses(
        input,
        listOf("Lcom/example/Missing;"),
        entrySize = dex.size.toLong()
      )
    }
    assertEquals(DEX_HEADER_SIZE.toLong(), input.bytesRead)
  }

  private fun createDex(vararg classDefs: ImmutableClassDef): ByteArray {
    val dexFile = temporaryFolder.newFile()
    DexPool.writeTo(
      dexFile.path,
      ImmutableDexFile(Opcodes.getDefault(), classDefs.toList())
    )
    return dexFile.readBytes()
  }

  private fun classWithStringConstants(values: List<String>): ImmutableClassDef {
    val fields = values.mapIndexed { index, value ->
      ImmutableField(
        "Lcom/example/Obfuscated;",
        "value$index",
        "Ljava/lang/String;",
        STATIC_FINAL_ACCESS_FLAGS,
        ImmutableStringEncodedValue(value),
        null,
        null
      )
    }
    return ImmutableClassDef(
      "Lcom/example/Obfuscated;",
      0x1,
      "Ljava/lang/Object;",
      null,
      null,
      null,
      fields,
      null
    )
  }

  private fun createDex41(descriptor: String): ByteArray {
    val descriptorBytes = descriptor.toByteArray()
    val stringIdsOffset = DEX_CONTAINER_HEADER_SIZE
    val typeIdsOffset = stringIdsOffset + UINT_SIZE
    val classDefsOffset = typeIdsOffset + UINT_SIZE
    val descriptorOffset = classDefsOffset + CLASS_DEF_ITEM_SIZE
    val dex = ByteArray(descriptorOffset + 1 + descriptorBytes.size + 1)
    "dex\n041\u0000".toByteArray().copyInto(dex)
    dex.writeIntLe(FILE_SIZE_OFFSET, dex.size)
    dex.writeIntLe(HEADER_SIZE_OFFSET, DEX_CONTAINER_HEADER_SIZE)
    dex.writeIntLe(ENDIAN_TAG_OFFSET, ENDIAN_CONSTANT)
    dex.writeIntLe(STRING_IDS_SIZE_OFFSET, 1)
    dex.writeIntLe(STRING_IDS_OFFSET_OFFSET, stringIdsOffset)
    dex.writeIntLe(TYPE_IDS_SIZE_OFFSET, 1)
    dex.writeIntLe(TYPE_IDS_OFFSET_OFFSET, typeIdsOffset)
    dex.writeIntLe(CLASS_DEFS_SIZE_OFFSET, 1)
    dex.writeIntLe(CLASS_DEFS_OFFSET_OFFSET, classDefsOffset)
    dex.writeIntLe(CONTAINER_SIZE_OFFSET, dex.size)
    dex.writeIntLe(HEADER_OFFSET_OFFSET, 0)
    dex.writeIntLe(stringIdsOffset, descriptorOffset)
    dex.writeIntLe(typeIdsOffset, 0)
    dex.writeIntLe(classDefsOffset, 0)
    dex[descriptorOffset] = descriptor.length.toByte()
    descriptorBytes.copyInto(dex, descriptorOffset + 1)
    return dex
  }

  private fun createTruncatedDex(): ByteArray {
    val dex = ByteArray(DEX_HEADER_SIZE + UINT_SIZE * 2)
    "dex\n035\u0000".toByteArray().copyInto(dex)
    dex.writeIntLe(FILE_SIZE_OFFSET, dex.size + CLASS_DEF_ITEM_SIZE)
    dex.writeIntLe(HEADER_SIZE_OFFSET, DEX_HEADER_SIZE)
    dex.writeIntLe(ENDIAN_TAG_OFFSET, ENDIAN_CONSTANT)
    dex.writeIntLe(STRING_IDS_SIZE_OFFSET, 1)
    dex.writeIntLe(STRING_IDS_OFFSET_OFFSET, DEX_HEADER_SIZE)
    dex.writeIntLe(TYPE_IDS_SIZE_OFFSET, 1)
    dex.writeIntLe(TYPE_IDS_OFFSET_OFFSET, DEX_HEADER_SIZE + UINT_SIZE)
    dex.writeIntLe(CLASS_DEFS_SIZE_OFFSET, 1)
    dex.writeIntLe(CLASS_DEFS_OFFSET_OFFSET, dex.size)
    return dex
  }

  private fun ByteArray.writeIntLe(offset: Int, value: Int) {
    this[offset] = value.toByte()
    this[offset + 1] = (value ushr 8).toByte()
    this[offset + 2] = (value ushr 16).toByte()
    this[offset + 3] = (value ushr 24).toByte()
  }

  private class CountingInputStream(input: ByteArrayInputStream) : FilterInputStream(input) {
    var bytesRead: Long = 0
      private set

    override fun read(): Int {
      return super.read().also { if (it >= 0) bytesRead++ }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
      return super.read(buffer, offset, length).also { if (it > 0) bytesRead += it }
    }

    override fun skip(byteCount: Long): Long {
      return super.skip(byteCount).also { bytesRead += it }
    }
  }

  private companion object {
    const val DEX_HEADER_SIZE = 0x70
    const val DEX_CONTAINER_HEADER_SIZE = 0x78
    const val FILE_SIZE_OFFSET = 0x20
    const val HEADER_SIZE_OFFSET = 0x24
    const val ENDIAN_TAG_OFFSET = 0x28
    const val STRING_IDS_SIZE_OFFSET = 0x38
    const val STRING_IDS_OFFSET_OFFSET = 0x3c
    const val TYPE_IDS_SIZE_OFFSET = 0x40
    const val TYPE_IDS_OFFSET_OFFSET = 0x44
    const val CLASS_DEFS_SIZE_OFFSET = 0x60
    const val CLASS_DEFS_OFFSET_OFFSET = 0x64
    const val CONTAINER_SIZE_OFFSET = 0x70
    const val HEADER_OFFSET_OFFSET = 0x74
    const val ENDIAN_CONSTANT = 0x12345678
    const val UINT_SIZE = 4
    const val CLASS_DEF_ITEM_SIZE = 32
    const val STATIC_FINAL_ACCESS_FLAGS = 0x18
    const val MAX_DESCRIPTOR_BYTES = 1024 * 1024
    val KOTLIN_RUNTIME_MARKERS = listOf(
      "kotlin.jvm.functions.Function1",
      "kotlin.coroutines.jvm.internal.BaseContinuationImpl"
    )
  }
}
