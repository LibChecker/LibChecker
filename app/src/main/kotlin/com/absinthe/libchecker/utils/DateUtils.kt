package com.absinthe.libchecker.utils

import android.icu.util.Calendar
import android.icu.util.ChineseCalendar
import android.icu.util.GregorianCalendar
import com.absinthe.libchecker.annotation.AUTUMN
import com.absinthe.libchecker.annotation.SPRING
import com.absinthe.libchecker.annotation.SUMMER
import com.absinthe.libchecker.annotation.WINTER
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
  // ISO 8601 constants
  private const val ISO_8601_PATTERN_1 = "yyyy-MM-dd'T'HH:mm:ssZ"
  private const val ISO_8601_PATTERN_2 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  private val SUPPORTED_ISO_8601_PATTERNS = arrayOf(ISO_8601_PATTERN_1, ISO_8601_PATTERN_2)
  private const val TICK_MARK_COUNT = 2
  private const val COLON_PREFIX_COUNT = "+00".length

  /**
   * Parses a date from the specified ISO 8601-compliant string.
   *
   * @param string the string to parse
   * @return the [Date] resulting from the parsing, or null if the string could not be
   * parsed
   */
  fun parseIso8601DateTime(string: String): Date? {
    val s = string.replace("Z", "+00:00")
    for (pattern in SUPPORTED_ISO_8601_PATTERNS) {
      var str = s
      val colonPosition = pattern.lastIndexOf('Z') - TICK_MARK_COUNT + COLON_PREFIX_COUNT
      if (str.length > colonPosition) {
        str = str.substring(0, colonPosition) + str.substring(colonPosition + 1)
      }
      try {
        return SimpleDateFormat(pattern, Locale.US).parse(str)
      } catch (e: ParseException) {
        // try the next one
      }
    }
    return null
  }

  fun isChristmas(): Boolean {
    val today = Calendar.getInstance()
    val month = today.get(Calendar.MONTH)
    val date = today.get(Calendar.DATE)
    return month == Calendar.DECEMBER && date == 25
  }

  fun isChineseNewYearEve(): Boolean {
    val calendar = ChineseCalendar()
    val date = calendar.get(Calendar.DATE)
    val month = calendar.get(Calendar.MONTH)
    val lastDay = calendar.getActualMaximum(Calendar.DATE)
    return month == Calendar.DECEMBER && date == lastDay
  }

  fun isChineseNewYear(): Boolean {
    val calendar = ChineseCalendar()
    val date = calendar.get(Calendar.DATE)
    val month = calendar.get(Calendar.MONTH)
    return month == Calendar.JANUARY && date == 1
  }

  fun getChineseZodiac(): String {
    val cc = Calendar.getInstance(Locale.CHINA) as GregorianCalendar
    val animalIndex = cc.get(Calendar.YEAR) % 12
    val zodiacList = listOf("🐒", "🐔", "🐶", "🐷", "🐭", "🐮", "🐯", "🐰", "🐲", "🐍", "🐴", "🐑", "🐒", "🐔", "🐶", "🐷")
    return zodiacList[animalIndex]
  }

  fun getToday(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DATE)
    return "$year.$month.$day"
  }

  fun isTimestampToday(timestamp: Long): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    val today = Calendar.getInstance()
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)

    val timestampYear = calendar.get(Calendar.YEAR)
    val timestampMonth = calendar.get(Calendar.MONTH)
    val timestampDay = calendar.get(Calendar.DAY_OF_MONTH)

    return todayYear == timestampYear && todayMonth == timestampMonth && todayDay == timestampDay
  }

  fun isTimestampThisMonth(timestamp: Long): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    val today = Calendar.getInstance()
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)

    val timestampYear = calendar.get(Calendar.YEAR)
    val timestampMonth = calendar.get(Calendar.MONTH)

    return todayYear == timestampYear && todayMonth == timestampMonth
  }

  fun getCurrentSeason(): Int {
    return when (java.util.Calendar.getInstance(Locale.getDefault()).get(java.util.Calendar.MONTH) + 1) {
      3, 4, 5 -> SPRING
      6, 7, 8 -> SUMMER
      9, 10, 11 -> AUTUMN
      12, 1, 2 -> WINTER
      else -> -1
    }
  }

  fun getCurrentSeasonString(season: Int = getCurrentSeason()): String {
    return when (season) {
      SPRING -> "Spring"
      SUMMER -> "Summer"
      AUTUMN -> "Autumn"
      WINTER -> "Winter"
      else -> ""
    }
  }

  fun getNextSeasonString(): String {
    return getCurrentSeasonString((getCurrentSeason() + 1) % 4)
  }
}
