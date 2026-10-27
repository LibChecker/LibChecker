package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileSnapshotDetailPayloadRepositoryTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun repeatedLargePayloadsStayWithinByteBudgetAndKeepNewest() = runBlocking {
    val directory = temporaryFolder.newFolder()
    val repository = FileSnapshotDetailPayloadRepository(directory)
    val item = snapshotDiffItem().copy(
      activitiesDiff = SnapshotDiffItem.DiffNode("old".repeat(400000), "new".repeat(400000))
    )
    val first = repository.save(item)
    assertTrue(File(directory, first).setLastModified(System.currentTimeMillis() - 10000))
    var newest = first
    repeat(15) { newest = repository.save(item) }
    assertTrue(directory.listFiles()!!.sumOf { it.length() } <= 32L * 1024 * 1024)
    assertNull(repository.load(first))
    assertEquals(item, repository.load(newest))
  }

  @Test fun smallPayloadsStayWithinFileCountBudget() = runBlocking {
    val directory = temporaryFolder.newFolder()
    val repository = FileSnapshotDetailPayloadRepository(directory)
    val first = repository.save(snapshotDiffItem())
    assertTrue(File(directory, first).setLastModified(System.currentTimeMillis() - 10000))
    var newest = first
    repeat(70) { newest = repository.save(snapshotDiffItem()) }
    assertTrue(directory.listFiles()!!.size <= 64)
    assertNull(repository.load(first))
    assertEquals(snapshotDiffItem(), repository.load(newest))
  }

  @Test fun oversizedPayloadFailsWithoutLeavingPartialFileOrEvictingRecentData() = runBlocking {
    val directory = temporaryFolder.newFolder()
    val repository = FileSnapshotDetailPayloadRepository(directory)
    val id = repository.save(snapshotDiffItem())
    try {
      repository.save(snapshotDiffItem().copy(metadataDiff = SnapshotDiffItem.DiffNode("x".repeat(33 * 1024 * 1024))))
      fail("Expected oversized payload rejection")
    } catch (_: java.io.IOException) {
      assertEquals(listOf(id), directory.listFiles()!!.map { it.name })
      assertEquals(snapshotDiffItem(), repository.load(id))
    }
  }

  @Test fun largePayloadSurvivesRepositoryRecreationAndRepeatedReads() = runBlocking {
    val directory = temporaryFolder.newFolder()
    val item = snapshotDiffItem().copy(
      activitiesDiff = SnapshotDiffItem.DiffNode("old".repeat(400000), "new".repeat(400000)),
      metadataDiff = SnapshotDiffItem.DiffNode("企业微信", "WeCom"),
      moved = 19,
      isTrackItem = true
    )
    val id = FileSnapshotDetailPayloadRepository(directory).save(item)
    assertEquals(36, id.length)
    assertTrue(File(directory, id).length() > 2000000)
    val recreated = FileSnapshotDetailPayloadRepository(directory)
    assertEquals(item, recreated.load(id))
    assertEquals(item, recreated.load(id))
    val second = recreated.save(snapshotDiffItem())
    assertNotEquals(id, second)
    assertEquals(item, recreated.load(id))
  }

  @Test fun missingAndInvalidIdsDoNotReadOutsideCache() = runBlocking {
    val repository = FileSnapshotDetailPayloadRepository(temporaryFolder.newFolder())
    assertNull(repository.load("../outside"))
    assertNull(repository.load("00000000-0000-0000-0000-000000000000"))
  }

  @Test fun oldPayloadsArePrunedButRecentOnesRemain() = runBlocking {
    val directory = temporaryFolder.newFolder()
    val repository = FileSnapshotDetailPayloadRepository(directory)
    val old = repository.save(snapshotDiffItem())
    assertTrue(File(directory, old).setLastModified(1))
    val recent = repository.save(snapshotDiffItem())
    assertNull(repository.load(old))
    assertEquals(snapshotDiffItem(), repository.load(recent))
  }

  @Test fun failedWriteDoesNotReturnAnId() = runBlocking {
    val repository = FileSnapshotDetailPayloadRepository(temporaryFolder.newFile())
    try {
      repository.save(snapshotDiffItem())
      fail("Expected an unwritable cache to fail")
    } catch (_: IllegalStateException) {
      // No navigation token is returned for a failed write.
    }
  }

  @Test fun corruptPayloadIsReportedInsteadOfReturningPartialData() = runBlocking {
    val directory = temporaryFolder.newFolder()
    val repository = FileSnapshotDetailPayloadRepository(directory)
    val id = repository.save(snapshotDiffItem())
    File(directory, id).writeText("{")
    try {
      repository.load(id)
      fail("Expected a corrupt payload to fail")
    } catch (_: java.io.IOException) {
      // The activity handles failed loads by showing an error and finishing.
    }
  }

  private fun snapshotDiffItem(dexInfo: String = "[]"): SnapshotDiffItem {
    return SnapshotDiffItem(
      packageName = "com.example",
      updateTime = 1,
      labelDiff = SnapshotDiffItem.DiffNode("Example", "Example"),
      versionNameDiff = SnapshotDiffItem.DiffNode("1", "1"),
      versionCodeDiff = SnapshotDiffItem.DiffNode(1, 1),
      abiDiff = SnapshotDiffItem.DiffNode(0, 0),
      targetApiDiff = SnapshotDiffItem.DiffNode(35, 35),
      compileSdkDiff = SnapshotDiffItem.DiffNode(35, 35),
      minSdkDiff = SnapshotDiffItem.DiffNode(24, 24),
      nativeLibsDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      servicesDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      activitiesDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      receiversDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      providersDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      permissionsDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      metadataDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      packageSizeDiff = SnapshotDiffItem.DiffNode(1, 1),
      dexInfoDiff = SnapshotDiffItem.DiffNode(dexInfo, dexInfo),
      resourcesSizeDiff = SnapshotDiffItem.DiffNode(1, 1),
      changed = 1
    )
  }
}
