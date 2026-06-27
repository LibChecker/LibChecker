package com.absinthe.libchecker.domain.snapshot.list.capture

interface SnapshotCaptureStateRepository {
  fun shouldSaveFullSnapshot(): Boolean

  fun markFullSnapshotSaved()
}
