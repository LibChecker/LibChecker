package com.absinthe.libchecker.domain.snapshot

interface SnapshotCaptureStateRepository {
  fun shouldSaveFullSnapshot(): Boolean

  fun markFullSnapshotSaved()
}
