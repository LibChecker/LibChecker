package com.absinthe.libchecker.domain.snapshot

interface SnapshotDatabaseFileRepository {
  fun getDatabaseSizeBytes(): Long
}
