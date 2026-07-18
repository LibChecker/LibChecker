package com.absinthe.libchecker.data.statistics

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfficialStatisticRemoteSourceTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `accepts an existing download directory`() {
    val directory = temporaryFolder.newFolder("download")

    assertTrue(directory.ensureDirectoryExists())
  }

  @Test
  fun `creates a missing download directory`() {
    val directory = File(temporaryFolder.root, "nested/download")

    assertTrue(directory.ensureDirectoryExists())
    assertTrue(directory.isDirectory)
  }
}
