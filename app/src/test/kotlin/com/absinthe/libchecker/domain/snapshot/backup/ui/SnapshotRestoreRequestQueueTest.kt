package com.absinthe.libchecker.domain.snapshot.backup.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotRestoreRequestQueueTest {

  @Test
  fun pickerDocumentUriWithoutFileExtensionIsConsumed() {
    val documentUri = "content://com.android.providers.downloads.documents/document/142"
    val pendingRestoreUris = ArrayDeque(listOf(documentUri))

    assertEquals(
      documentUri,
      takeNextPendingSnapshotRestoreRequest(
        pendingRestoreUris = pendingRestoreUris,
        activeRestoreUri = null
      )
    )
  }

  @Test
  fun activeRestoreKeepsPendingRequestQueued() {
    val pendingRestoreUris = ArrayDeque(listOf("pending"))

    assertNull(
      takeNextPendingSnapshotRestoreRequest(
        pendingRestoreUris = pendingRestoreUris,
        activeRestoreUri = "active"
      )
    )
    assertEquals(listOf("pending"), pendingRestoreUris.toList())
  }
}
