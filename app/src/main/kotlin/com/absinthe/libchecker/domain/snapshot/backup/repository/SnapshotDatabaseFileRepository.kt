package com.absinthe.libchecker.domain.snapshot.backup.repository

interface SnapshotDatabaseFileRepository {
  fun getDatabaseSizeBytes(): Long
}
