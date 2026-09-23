package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtoSnapshotArchiveCodecTest {

  @Test
  fun `round trip preserves archived state`() {
    val codec = ProtoSnapshotArchiveCodec()
    val output = ByteArrayOutputStream()

    codec.write(snapshotItem(), output)
    val restored = codec.read(ByteArrayInputStream(output.toByteArray()))

    assertTrue(restored?.isArchived == true)
    assertEquals("", restored?.versionName)
    assertEquals(
      "[{\"name\":\"base/classes.dex\",\"size\":10,\"classCount\":1,\"crc32\":1}]",
      restored?.dexInfo
    )
    assertEquals(
      "[{\"name\":\"base/resources.arsc\",\"size\":42,\"crc32\":2}]",
      restored?.resourceInfo
    )
    assertEquals(42L, restored?.resourcesSize)
    assertEquals(SnapshotItem.CURRENT_STATS_VERSION, restored?.statsVersion)
  }

  @Test
  fun `invalid stored stats are downgraded on restore`() {
    val codec = ProtoSnapshotArchiveCodec()
    val output = ByteArrayOutputStream()
    val invalid = snapshotItem().copy(
      dexInfo = "[{\"name\":\"../classes.dex\",\"size\":10,\"classCount\":1,\"crc32\":1}]",
      resourcesSize = Long.MAX_VALUE
    )

    codec.write(invalid, output)
    val restored = codec.read(ByteArrayInputStream(output.toByteArray()))

    assertFalse(restored == null)
    assertEquals("[]", restored?.dexInfo)
    assertEquals(0L, restored?.resourcesSize)
    assertEquals(SnapshotItem.CURRENT_STATS_VERSION, restored?.statsVersion)
    assertFalse(restored?.dexStatsAvailable == true)
    assertFalse(restored?.resourceStatsAvailable == true)
  }

  @Test
  fun `oversized dex json is dropped without discarding valid resources`() {
    val codec = ProtoSnapshotArchiveCodec()
    val output = ByteArrayOutputStream()
    val oversized = snapshotItem().copy(
      dexInfo = "[]".padEnd(70_000, ' ')
    )

    codec.write(oversized, output)
    val restored = codec.read(ByteArrayInputStream(output.toByteArray()))

    assertEquals("[]", restored?.dexInfo)
    assertFalse(restored?.dexStatsAvailable == true)
    assertEquals(
      "[{\"name\":\"base/resources.arsc\",\"size\":42,\"crc32\":2}]",
      restored?.resourceInfo
    )
    assertTrue(restored?.resourceStatsAvailable == true)
  }

  @Test
  fun `oversized snapshot record is rejected before parsing`() {
    val codec = ProtoSnapshotArchiveCodec()
    val output = ByteArrayOutputStream()

    codec.write(snapshotItem().copy(label = "x".repeat(17 * 1024 * 1024)), output)

    assertThrows(IllegalArgumentException::class.java) {
      codec.read(ByteArrayInputStream(output.toByteArray()))
    }
  }

  @Test
  fun `consecutive records survive short reads and end at EOF`() {
    val codec = ProtoSnapshotArchiveCodec()
    val items = listOf(snapshotItem(), snapshotItem().copy(packageName = "com.example.second"))
    val output = ByteArrayOutputStream()
    items.forEach { codec.write(it, output) }
    var closed = false
    val input = object : ByteArrayInputStream(output.toByteArray()) {
      override fun read(buffer: ByteArray, offset: Int, length: Int): Int = super.read(buffer, offset, minOf(length, 3))

      override fun close() {
        closed = true
      }
    }

    items.forEach { assertEquals(it, codec.read(input)) }
    assertNull(codec.read(input))
    assertFalse(closed)
    assertNull(codec.read(ByteArrayInputStream(byteArrayOf())))
  }

  @Test
  fun `truncated and malformed frames are rejected`() {
    val codec = ProtoSnapshotArchiveCodec()
    val output = ByteArrayOutputStream()
    codec.write(snapshotItem(), output)

    listOf(
      byteArrayOf(0x80.toByte()),
      ByteArray(10) { 0x80.toByte() },
      output.toByteArray().dropLast(1).toByteArray()
    ).forEach { bytes ->
      assertThrows(IOException::class.java) {
        codec.read(ByteArrayInputStream(bytes))
      }
    }
    assertThrows(IllegalArgumentException::class.java) {
      codec.read(ByteArrayInputStream(byteArrayOf(-1, -1, -1, -1, 0x0f)))
    }
  }

  @Test
  fun `overlong valid length prefix remains compatible`() {
    val codec = ProtoSnapshotArchiveCodec()
    val input = ByteArrayInputStream(ByteArray(9) { 0x80.toByte() } + byteArrayOf(0, 0))

    assertEquals(codec.read(ByteArrayInputStream(byteArrayOf(0))), codec.read(input))
    assertEquals(1, input.available())
    assertFalse(codec.read(input) == null)
    assertNull(codec.read(input))
  }

  private fun snapshotItem(): SnapshotItem {
    return SnapshotItem(
      id = null,
      packageName = "com.example",
      timeStamp = 1L,
      label = "Example",
      versionName = "",
      versionCode = 3022L,
      isArchived = true,
      installedTime = 2L,
      lastUpdatedTime = 3L,
      isSystem = false,
      abi = 3,
      targetApi = 35,
      nativeLibs = "[]",
      services = "[]",
      activities = "[]",
      receivers = "[]",
      providers = "[]",
      permissions = "[]",
      metadata = "[]",
      packageSize = 0L,
      compileSdk = 35,
      minSdk = 24,
      dexInfo = "[{\"name\":\"base/classes.dex\",\"size\":10,\"classCount\":1,\"crc32\":1}]",
      resourceInfo = "[{\"name\":\"base/resources.arsc\",\"size\":42,\"crc32\":2}]",
      resourcesSize = 42L,
      statsVersion = SnapshotItem.CURRENT_STATS_VERSION,
      dexStatsAvailable = true,
      resourceStatsAvailable = true
    )
  }
}
