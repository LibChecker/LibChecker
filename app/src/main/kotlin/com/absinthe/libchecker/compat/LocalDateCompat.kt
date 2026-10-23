package com.absinthe.libchecker.compat

import android.os.Build
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** A civil date stored independently of time zones and daylight-saving transitions. */
data class LocalDateCompat(val epochDay: Long) : Comparable<LocalDateCompat> {
  val dayOfWeek: Int
    get() = Math.floorMod(epochDay + 3, 7L).toInt() + 1

  fun plusDays(days: Long): LocalDateCompat = LocalDateCompat(epochDay + days)

  fun minusDays(days: Long): LocalDateCompat = plusDays(-days)

  fun daysUntil(other: LocalDateCompat): Long = other.epochDay - epochDay

  fun isAfter(other: LocalDateCompat): Boolean = this > other

  fun isBefore(other: LocalDateCompat): Boolean = this < other

  override fun compareTo(other: LocalDateCompat): Int = epochDay.compareTo(other.epochDay)

  fun formatIso(locale: Locale = Locale.getDefault()): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("yyyy-MM-dd", locale))
    } else {
      formatLegacy(SimpleDateFormat("yyyy-MM-dd", locale))
    }
  }

  fun formatMedium(locale: Locale = Locale.getDefault()): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    } else {
      formatLegacy(DateFormat.getDateInstance(DateFormat.MEDIUM, locale))
    }
  }

  private fun formatLegacy(formatter: DateFormat): String {
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(epochDay * MILLIS_PER_DAY))
  }

  override fun toString(): String = formatIso(Locale.US)

  companion object {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun now(timeZone: TimeZone = TimeZone.getDefault()): LocalDateCompat = fromTimestamp(System.currentTimeMillis(), timeZone)

    fun fromTimestamp(timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): LocalDateCompat {
      val epochDay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Instant.ofEpochMilli(timestamp).atZone(timeZone.toZoneId()).toLocalDate().toEpochDay()
      } else {
        // Split before adding the offset so imported Long.MIN/MAX timestamps cannot overflow.
        Math.floorDiv(timestamp, MILLIS_PER_DAY) +
          Math.floorDiv(Math.floorMod(timestamp, MILLIS_PER_DAY) + timeZone.getOffset(timestamp), MILLIS_PER_DAY)
      }
      return LocalDateCompat(epochDay)
    }
  }
}
