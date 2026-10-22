package com.absinthe.libchecker.compat

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDateCompatTest {
  @Test
  fun `legacy conversion agrees with java time across offsets DST and extreme timestamps`() {
    val timestamps = listOf(
      Long.MIN_VALUE,
      -1L,
      0L,
      Instant.parse("2024-03-10T09:59:59Z").toEpochMilli(),
      Instant.parse("2024-03-10T10:00:00Z").toEpochMilli(),
      Instant.parse("2024-11-03T09:00:00Z").toEpochMilli(),
      Long.MAX_VALUE
    )
    listOf("UTC", "America/Los_Angeles", "Asia/Shanghai", "Pacific/Kiritimati").forEach { id ->
      timestamps.forEach { timestamp ->
        if (id != "UTC" && timestamp in listOf(Long.MIN_VALUE, Long.MAX_VALUE)) return@forEach
        val expected = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of(id)).toLocalDate()
        val actual = LocalDateCompat.fromTimestamp(timestamp, TimeZone.getTimeZone(id))
        assertEquals("$id / $timestamp", expected.toEpochDay(), actual.epochDay)
        assertEquals(expected.dayOfWeek.value, actual.dayOfWeek)
      }
    }
  }

  @Test
  fun `date arithmetic and formatting preserve leap days independent of default zone`() {
    val original = TimeZone.getDefault()
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
      val february = LocalDateCompat(LocalDate.of(2024, 2, 28).toEpochDay())
      assertEquals("2024-02-29", february.plusDays(1).formatIso(Locale.US))
      assertEquals("Mar 1, 2024", february.plusDays(2).formatMedium(Locale.US))
      assertEquals(2L, february.daysUntil(february.plusDays(2)))
      assertEquals(february, february.plusDays(2).minusDays(2))
    } finally {
      TimeZone.setDefault(original)
    }
  }
}
