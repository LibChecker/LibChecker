package com.absinthe.libchecker.domain.snapshot.display

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormatSnapshotTimestampUseCase {

  private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat {
      return SimpleDateFormat(DISPLAY_PATTERN, Locale.getDefault())
    }
  }

  operator fun invoke(timestamp: Long): String {
    val formatter = dateFormat.get() ?: SimpleDateFormat(DISPLAY_PATTERN, Locale.getDefault())
    return formatter.format(Date(timestamp))
  }

  private companion object {
    private const val DISPLAY_PATTERN = "yyyy-MM-dd, HH:mm:ss"
  }
}
