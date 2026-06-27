package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.domain.snapshot.SnapshotCaptureStateRepository
import jonathanfinerty.once.Once

class OnceSnapshotCaptureStateRepository : SnapshotCaptureStateRepository {
  override fun shouldSaveFullSnapshot(): Boolean {
    return !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.SHOULD_SAVE_FULL_SNAPSHOT)
  }

  override fun markFullSnapshotSaved() {
    Once.markDone(OnceTag.SHOULD_SAVE_FULL_SNAPSHOT)
  }
}
