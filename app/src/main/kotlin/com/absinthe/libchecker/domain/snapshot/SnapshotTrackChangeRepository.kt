package com.absinthe.libchecker.domain.snapshot

interface SnapshotTrackChangeRepository {
  fun markChanged()
  fun consumeChanged(): Boolean
}
