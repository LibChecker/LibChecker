package com.absinthe.libchecker.utils.apk

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class APKSParserTest {

  @Test
  fun `manifest split name is stable across archive entry renames`() {
    val oldNames = resolveApksSplitNames(listOf(File("split_feature.apk"))) { "feature" }
    val newNames = resolveApksSplitNames(listOf(File("split_feature_v2.apk"))) { "feature" }

    assertArrayEquals(oldNames, newNames)
  }
}
