package com.absinthe.libchecker.domain.snapshot.backup.usecase

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun buildSnapshotBackupFileName(extension: String): String {
  val formatted = SimpleDateFormat(
    "yyyy-MM-dd-HH-mm-ss",
    Locale.getDefault()
  ).format(Date())
  return "LibChecker-Snapshot-Backups-$formatted.$extension"
}
