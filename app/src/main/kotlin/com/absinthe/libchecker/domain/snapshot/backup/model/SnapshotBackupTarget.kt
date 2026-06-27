package com.absinthe.libchecker.domain.snapshot.backup.model

sealed interface SnapshotBackupTarget {
  data class Archive(val fileName: String) : SnapshotBackupTarget
  data object Database : SnapshotBackupTarget
}
