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
  private val SUPPORTED_ISO_8601_PATTERNS = arrayOf("yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  private val iso8601Formatters = object : ThreadLocal<Array<SimpleDateFormat>>() {
    override fun initialValue(): Array<SimpleDateFormat> {
      return Array(SUPPORTED_ISO_8601_PATTERNS.size) {
        SimpleDateFormat(SUPPORTED_ISO_8601_PATTERNS[it], Locale.US)
      }
    }
  }

  /**
   * Parses a date from the specified ISO 8601-compliant string.
   *
   * @param string the string to parse
   * @return the [Date] resulting from the parsing, or null if the string could not be
   * parsed
   */
  fun parseIso8601DateTime(string: String): Date? {
    val s = string.replace("Z", "+00:00")
    val formatters = iso8601Formatters.get()
    for (i in SUPPORTED_ISO_8601_PATTERNS.indices) {
      val pattern = SUPPORTED_ISO_8601_PATTERNS[i]
      var str = s
      val colonPosition = pattern.lastIndexOf('Z') + 1
      if (str.length > colonPosition) {
        str = str.substring(0, colonPosition) + str.substring(colonPosition + 1)
      }
      try {
        val formatter = formatters?.getOrNull(i) ?: SimpleDateFormat(pattern, Locale.US)
        return formatter.parse(str)
      } catch (e: ParseException) {
        // try the next one
      }
    }
    return null
  }

  fun getHolidayEmoji(): String? {
    val today = Calendar.getInstance()
    val month = today.get(Calendar.MONTH)
    val date = today.get(Calendar.DATE)
    if (month == Calendar.DECEMBER && date == 25) {
      return "\uD83C\uDF84"
    }
    if (month == Calendar.JANUARY || month == Calendar.FEBRUARY) {
      val calendar = ChineseCalendar()
      val cMonth = calendar.get(Calendar.MONTH)
      val cDate = calendar.get(Calendar.DATE)
      if (cMonth == Calendar.DECEMBER && cDate == calendar.getActualMaximum(Calendar.DATE)) {
        return "\uD83C\uDFEE"
      }
      if (cMonth == Calendar.JANUARY && cDate == 1) {
        val animalIndex = today.get(Calendar.YEAR) % 12
        return ZODIAC_LIST.getOrNull(animalIndex)
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
    val today = Calendar.getInstance()
    val gMonth = today.get(Calendar.MONTH)
    if (gMonth != Calendar.JANUARY && gMonth != Calendar.FEBRUARY) return false
    val calendar = ChineseCalendar()
    val date = calendar.get(Calendar.DATE)
    val month = calendar.get(Calendar.MONTH)
    val lastDay = calendar.getActualMaximum(Calendar.DATE)
    return month == Calendar.DECEMBER && date == lastDay
  }

  fun isChineseNewYear(): Boolean {
    val today = Calendar.getInstance()
    val gMonth = today.get(Calendar.MONTH)
    if (gMonth != Calendar.JANUARY && gMonth != Calendar.FEBRUARY) return false
    val calendar = ChineseCalendar()
    val date = calendar.get(Calendar.DATE)
    val month = calendar.get(Calendar.MONTH)
    return month == Calendar.JANUARY && date == 1
  }

  private val ZODIAC_LIST = arrayOf("🐒", "🐔", "🐶", "🐷", "🐭", "🐮", "🐯", "🐰", "🐲", "🐍", "🐴", "🐑", "🐒", "🐔", "🐶", "🐷")

  fun getChineseZodiac(): String {
    val cc = Calendar.getInstance(Locale.CHINA) as GregorianCalendar
    val animalIndex = cc.get(Calendar.YEAR) % 12
    return ZODIAC_LIST[animalIndex]
  }

  fun getToday(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DATE)
    return "$year.$month.$day"
  }

  fun isTimestampToday(timestamp: Long): Boolean {
    return android.text.format.DateUtils.isToday(timestamp)
  }

  fun isTimestampThisMonth(timestamp: Long): Boolean {
    val calendar = Calendar.getInstance()
    val todayYear = calendar.get(Calendar.YEAR)
    val todayMonth = calendar.get(Calendar.MONTH)

    calendar.timeInMillis = timestamp
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
