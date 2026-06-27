package com.absinthe.libchecker.domain.snapshot.track.repository

interface SnapshotTrackChangeRepository {
  fun markChanged()
  fun consumeChanged(): Boolean
}
