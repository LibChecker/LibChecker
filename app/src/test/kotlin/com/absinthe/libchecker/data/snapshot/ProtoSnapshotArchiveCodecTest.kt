package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
