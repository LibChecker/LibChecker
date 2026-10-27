package com.absinthe.libchecker.domain.snapshot.detail.repository

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem

interface SnapshotDetailPayloadRepository {
  suspend fun save(item: SnapshotDiffItem): String
  suspend fun load(id: String): SnapshotDiffItem?
}
