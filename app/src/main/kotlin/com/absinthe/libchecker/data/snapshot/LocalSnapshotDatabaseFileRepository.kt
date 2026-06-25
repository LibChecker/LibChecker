package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotDatabaseFileRepository
import com.absinthe.libchecker.utils.FileUtils

class LocalSnapshotDatabaseFileRepository : SnapshotDatabaseFileRepository {

  override fun getDatabaseSizeBytes(): Long {
    return FileUtils.getFileSize(RulesRepository.getDatabaseFile())
  }
}
