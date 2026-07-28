package com.absinthe.libchecker.data.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidStatisticEvidenceRepositoryTest {

  @Test
  fun `resolves literal manifest Boolean values`() {
    assertEquals(true, resolveManifestBooleanValue(true) { null })
    assertEquals(true, resolveManifestBooleanValue("true") { null })
    assertEquals(false, resolveManifestBooleanValue("false") { null })
    assertEquals(true, resolveManifestBooleanValue("1") { null })
    assertEquals(false, resolveManifestBooleanValue("0") { null })
  }

  @Test
  fun `resolves referenced manifest Boolean resources`() {
    assertEquals(
      true,
      resolveManifestBooleanValue(0x7f050001) { resourceId ->
        resourceId == 0x7f050001
      }
    )
  }

  @Test
  fun `does not infer missing or invalid manifest values`() {
    assertNull(resolveManifestBooleanValue(null) { false })
    assertNull(resolveManifestBooleanValue("enabled") { false })
  }
}
