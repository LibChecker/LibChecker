package com.absinthe.libchecker.domain.snapshot.display

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatSnapshotTimestampUseCaseTest {
  @Test
  fun `same instance follows changes to the default time zone`() {
    val originalLocale = Locale.getDefault()
    val originalTimeZone = TimeZone.getDefault()
    try {
      Locale.setDefault(Locale.US)
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
      val format = FormatSnapshotTimestampUseCase()
      assertEquals("2024-01-01, 00:00:00", format(1704067200000L))
      TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
      assertEquals("2024-01-01, 08:00:00", format(1704067200000L))
    } finally {
      Locale.setDefault(originalLocale)
      TimeZone.setDefault(originalTimeZone)
    }
  }
}
