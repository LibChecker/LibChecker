package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import java.io.InputStream
import java.io.OutputStream

interface SnapshotArchiveCodec {
  /** Returns null when the archive stream is exhausted. */
  fun read(inputStream: InputStream): SnapshotItem?
  fun write(item: SnapshotItem, outputStream: OutputStream)
}
