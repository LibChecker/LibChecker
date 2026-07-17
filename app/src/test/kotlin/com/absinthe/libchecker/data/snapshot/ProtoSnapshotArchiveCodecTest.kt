package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
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
      minSdk = 24
    )
  }
}
