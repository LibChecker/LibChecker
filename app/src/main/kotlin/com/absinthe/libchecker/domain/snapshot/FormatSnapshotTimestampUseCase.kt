package com.absinthe.libchecker.domain.snapshot

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormatSnapshotTimestampUseCase {

  operator fun invoke(timestamp: Long): String {
    return SimpleDateFormat(DISPLAY_PATTERN, Locale.getDefault())
      .format(Date(timestamp))
  }

  private companion object {
    private const val DISPLAY_PATTERN = "yyyy-MM-dd, HH:mm:ss"
  }
}
